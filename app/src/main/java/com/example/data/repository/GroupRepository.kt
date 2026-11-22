package com.example.data.repository

import com.example.domain.model.GroupChat
import com.example.domain.model.GroupMessage
import com.example.domain.model.MessageType
import com.example.security.InputValidator
import com.example.security.OperationType
import com.example.security.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GroupRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing groups.")

    fun observeMyGroups(): Flow<List<GroupChat>> = flow {
        val uid = requireUserId()
        val path = "groups"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupChat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastMessageTime?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeGroup(groupId: String): Flow<GroupChat?> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId"
        emitAll(
            db.collection("groups").document(groupId)
                .snapshots()
                .map { it.toObject(GroupChat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeGroupMessages(groupId: String, pageSize: Long = 50L): Flow<List<GroupMessage>> = flow {
        val uid = requireUserId()
        val path = "groups/$groupId/messages"
        emitAll(
            db.collection("groups").document(groupId).collection("messages")
                .whereArrayContains("memberIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.timestamp?.toDate()?.time ?: Long.MAX_VALUE }
                        .takeLast(pageSize.toInt().coerceAtLeast(1))
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createGroup(
        name: String,
        description: String,
        imageUrl: String?,
        memberIds: List<String>
    ): Result<String> {
        var path = "groups"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(InputValidator.MAX_GROUP_NAME)
            InputValidator.validateGroupName(cleanName)?.let {
                return Result.failure(IllegalArgumentException(it))
            }
            val members = (memberIds + uid).distinct().take(100)
            val ref = db.collection("groups").document()
            path = ref.path
            val payload = mutableMapOf<String, Any?>(
                "name" to cleanName,
                "description" to description.trim().take(InputValidator.MAX_GROUP_DESC),
                "imageUrl" to imageUrl,
                "memberIds" to members,
                "adminIds" to listOf(uid),
                "createdBy" to uid,
                "lastMessage" to "Group created",
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "mutedBy" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendGroupMessage(
        groupId: String,
        memberIds: List<String>,
        senderName: String,
        type: MessageType,
        text: String,
        mediaUrl: String? = null,
        mediaName: String? = null,
        mediaSizeBytes: Long? = null,
        voiceDurationMs: Long? = null,
        replyTo: GroupMessage? = null
    ): Result<Unit> {
        var path = "groups/$groupId/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(InputValidator.MAX_MESSAGE_TEXT)
            if (type == MessageType.TEXT && cleanText.isEmpty()) {
                return Result.failure(IllegalArgumentException("Message cannot be empty."))
            }
            val ref = db.collection("groups").document(groupId).collection("messages").document()
            path = ref.path
            val payload = mutableMapOf<String, Any?>(
                "groupId" to groupId,
                "senderId" to uid,
                "senderName" to senderName.take(80),
                "memberIds" to memberIds,
                "type" to type.name,
                "text" to cleanText,
                "mediaUrl" to mediaUrl,
                "mediaName" to mediaName?.take(255),
                "mediaSizeBytes" to mediaSizeBytes,
                "voiceDurationMs" to voiceDurationMs,
                "replyToId" to replyTo?.id,
                "replyToText" to replyTo?.text?.take(300),
                "replyToSender" to replyTo?.senderName?.take(80),
                "reactions" to emptyMap<String, String>(),
                "timestamp" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()

            val summary = when (type) {
                MessageType.TEXT -> cleanText.take(120)
                MessageType.IMAGE -> "Photo"
                MessageType.VIDEO -> "Video"
                MessageType.DOCUMENT -> mediaName ?: "Document"
                MessageType.AUDIO -> "Audio"
                MessageType.VOICE -> "Voice note"
            }
            db.collection("groups").document(groupId).update(
                mapOf(
                    "lastMessage" to summary,
                    "lastMessageSenderName" to senderName.take(80),
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleGroupReaction(groupId: String, message: GroupMessage, emoji: String): Result<Unit> {
        val path = "groups/$groupId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            val updated = message.reactions.toMutableMap()
            if (updated[uid] == emoji) updated.remove(uid) else updated[uid] = emoji
            db.document(path).update(
                mapOf("reactions" to updated, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGroupInfo(groupId: String, name: String, description: String): Result<Unit> {
        val path = "groups/$groupId"
        return try {
            requireUserId()
            val cleanName = name.trim().take(InputValidator.MAX_GROUP_NAME)
            InputValidator.validateGroupName(cleanName)?.let {
                return Result.failure(IllegalArgumentException(it))
            }
            db.document(path).update(
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(InputValidator.MAX_GROUP_DESC),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addMember(groupId: String, userId: String): Result<Unit> = updateGroupField(
        groupId,
        mapOf("memberIds" to FieldValue.arrayUnion(userId), "updatedAt" to FieldValue.serverTimestamp())
    )

    suspend fun removeMember(groupId: String, userId: String): Result<Unit> = updateGroupField(
        groupId,
        mapOf(
            "memberIds" to FieldValue.arrayRemove(userId),
            "adminIds" to FieldValue.arrayRemove(userId),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun setAdmin(groupId: String, userId: String, isAdmin: Boolean): Result<Unit> = updateGroupField(
        groupId,
        mapOf(
            "adminIds" to if (isAdmin) FieldValue.arrayUnion(userId) else FieldValue.arrayRemove(userId),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    suspend fun leaveGroup(groupId: String, currentMembers: List<String>, currentAdmins: List<String>): Result<Unit> {
        val path = "groups/$groupId"
        return try {
            val uid = requireUserId()
            val newMembers = currentMembers.filter { it != uid }
            if (newMembers.isEmpty()) {
                return deleteGroup(groupId)
            }
            val newAdmins = currentAdmins.filter { it != uid }.ifEmpty {
                listOf(newMembers.first())
            }
            db.document(path).update(
                mapOf(
                    "memberIds" to newMembers,
                    "adminIds" to newAdmins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteGroup(groupId: String): Result<Unit> {
        val path = "groups/$groupId"
        return try {
            requireUserId()
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun updateGroupField(groupId: String, updates: Map<String, Any>): Result<Unit> {
        val path = "groups/$groupId"
        return try {
            requireUserId()
            db.document(path).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
