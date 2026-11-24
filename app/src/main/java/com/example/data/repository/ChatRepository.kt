package com.example.data.repository

import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatRoom
import com.example.domain.model.DeliveryStatus
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

class ChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing chats.")

    fun deterministicChatId(uid1: String, uid2: String): String {
        val sorted = listOf(uid1, uid2).sorted()
        return InputValidator.sanitizeId("${sorted[0]}_${sorted[1]}")
    }

    fun observeMyChats(): Flow<List<ChatRoom>> = flow {
        val uid = requireUserId()
        val path = "chats"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<ChatRoom> { uid in it.pinnedBy }
                                .thenByDescending { it.lastMessageTime?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L }
                        )
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

    fun observeChatRoom(chatId: String): Flow<ChatRoom?> = flow {
        val uid = requireUserId()
        val path = "chats/$chatId"
        emitAll(
            db.collection("chats").document(chatId)
                .snapshots()
                .map { it.toObject(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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

    fun observeMessages(chatId: String, pageSize: Long = 40L): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    suspend fun getOrCreateChat(peerUid: String): Result<String> {
        var path = "chats"
        var operation = OperationType.GET
        return try {
            val uid = requireUserId()
            require(peerUid.isNotBlank() && peerUid != uid) { "Invalid chat recipient." }
            val chatId = deterministicChatId(uid, peerUid)
            val ref = db.collection("chats").document(chatId)
            path = ref.path
            val snap = ref.get().await()
            if (!snap.exists()) {
                operation = OperationType.CREATE
                val payload = mapOf(
                    "participantIds" to listOf(uid, peerUid).sorted(),
                    "createdBy" to uid,
                    "lastMessage" to "",
                    "lastMessageType" to MessageType.TEXT.name,
                    "typingUserIds" to emptyList<String>(),
                    "pinnedBy" to emptyList<String>(),
                    "mutedBy" to emptyList<String>(),
                    "unreadCounts" to mapOf(uid to 0L, peerUid to 0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                try {
                    ref.set(payload).await()
                } catch (createErr: FirebaseFirestoreException) {
                    val racedDoc = runCatching { ref.get().await() }.getOrNull()
                    if (racedDoc == null || !racedDoc.exists()) {
                        throw createErr
                    }
                }
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, operation, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        participantIds: List<String>,
        senderName: String,
        type: MessageType,
        text: String,
        mediaUrl: String? = null,
        mediaName: String? = null,
        mediaSizeBytes: Long? = null,
        voiceDurationMs: Long? = null,
        replyTo: ChatMessage? = null,
        forwarded: Boolean = false
    ): Result<Unit> {
        var path = "chats/$chatId/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(InputValidator.MAX_MESSAGE_TEXT)
            if (type == MessageType.TEXT && cleanText.isEmpty()) {
                return Result.failure(IllegalArgumentException("Message cannot be empty."))
            }

            val msgRef = db.collection("chats").document(chatId).collection("messages").document()
            path = msgRef.path

            val payload = InputValidator.nonNullFields(
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to senderName.take(80),
                "participantIds" to participantIds,
                "type" to type.name,
                "text" to cleanText,
                "mediaUrl" to mediaUrl,
                "mediaName" to mediaName?.take(255),
                "mediaSizeBytes" to mediaSizeBytes,
                "voiceDurationMs" to voiceDurationMs,
                "replyToId" to replyTo?.id,
                "replyToText" to replyTo?.text?.take(300),
                "replyToSender" to replyTo?.senderName?.take(80),
                "forwarded" to forwarded,
                "deliveryStatus" to DeliveryStatus.SENT.name,
                "reactions" to emptyMap<String, String>(),
                "timestamp" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            msgRef.set(payload).await()

            val summary = when (type) {
                MessageType.TEXT -> cleanText.take(120)
                MessageType.IMAGE -> "Photo"
                MessageType.VIDEO -> "Video"
                MessageType.DOCUMENT -> mediaName ?: "Document"
                MessageType.AUDIO -> "Audio file"
                MessageType.VOICE -> "Voice message"
            }

            val peerId = participantIds.firstOrNull { it != uid }
            val roomUpdates = mutableMapOf<String, Any>(
                "lastMessage" to summary,
                "lastMessageType" to type.name,
                "lastMessageSenderId" to uid,
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "typingUserIds" to FieldValue.arrayRemove(uid),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (peerId != null) {
                roomUpdates["unreadCounts.$peerId"] = FieldValue.increment(1L)
            }
            db.collection("chats").document(chatId).update(roomUpdates).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markChatRead(chatId: String, messages: List<ChatMessage>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            db.collection("chats").document(chatId).update(
                mapOf(
                    "unreadCounts.$uid" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val unreadIncoming = messages.filter {
                it.senderId != uid && it.status != DeliveryStatus.READ && it.id.isNotBlank()
            }
            if (unreadIncoming.isNotEmpty()) {
                val batch = db.batch()
                unreadIncoming.take(25).forEach { msg ->
                    val ref = db.collection("chats").document(chatId)
                        .collection("messages").document(msg.id)
                    batch.update(
                        ref,
                        mapOf(
                            "deliveryStatus" to DeliveryStatus.READ.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun setTyping(chatId: String, isTyping: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            val op = if (isTyping) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
            db.collection("chats").document(chatId).update(
                mapOf(
                    "typingUserIds" to op,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun togglePinChat(chatId: String, pinned: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val op = if (pinned) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
            db.collection("chats").document(chatId).update(
                mapOf("pinnedBy" to op, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun toggleMuteChat(chatId: String, muted: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val op = if (muted) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
            db.collection("chats").document(chatId).update(
                mapOf("mutedBy" to op, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String): Result<Unit> {
        val path = "chats/$chatId/messages/$messageId"
        return try {
            requireUserId()
            val clean = newText.trim().take(InputValidator.MAX_MESSAGE_TEXT)
            require(clean.isNotEmpty()) { "Message text cannot be empty." }
            db.document(path).update(
                mapOf(
                    "text" to clean,
                    "editedAt" to FieldValue.serverTimestamp(),
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

    suspend fun deleteMessageForEveryone(chatId: String, messageId: String): Result<Unit> {
        val path = "chats/$chatId/messages/$messageId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "text" to "This message was deleted",
                    "deletedAt" to FieldValue.serverTimestamp(),
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

    suspend fun toggleReaction(chatId: String, message: ChatMessage, emoji: String): Result<Unit> {
        val path = "chats/$chatId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            val updated = message.reactions.toMutableMap()
            if (updated[uid] == emoji) {
                updated.remove(uid)
            } else {
                updated[uid] = emoji
            }
            db.document(path).update(
                mapOf(
                    "reactions" to updated,
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
}
