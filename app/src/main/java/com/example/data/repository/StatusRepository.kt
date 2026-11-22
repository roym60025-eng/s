package com.example.data.repository

import com.example.domain.model.StatusPrivacy
import com.example.domain.model.StatusType
import com.example.domain.model.UserStatus
import com.example.security.InputValidator
import com.example.security.OperationType
import com.example.security.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class StatusRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing statuses.")

    fun observeActiveStatuses(): Flow<List<UserStatus>> = flow {
        val uid = requireUserId()
        val ownFlow = db.collection("statuses")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { it.toObjects(UserStatus::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        val publicFlow = db.collection("statuses")
            .whereEqualTo("privacy", StatusPrivacy.ALL_CONTACTS.name)
            .snapshots()
            .map { it.toObjects(UserStatus::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        emitAll(
            combine(ownFlow, publicFlow) { own, others ->
                val nowMs = System.currentTimeMillis()
                (own + others)
                    .distinctBy { it.id }
                    .filter { (it.expiresAt?.toDate()?.time ?: Long.MAX_VALUE) > nowMs }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: (error as? Exception)
                if (toLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(toLog, OperationType.LIST, "statuses")
                }
                throw error
            }
        )
    }

    suspend fun postStatus(
        userName: String,
        userPhotoUrl: String?,
        type: StatusType,
        caption: String?,
        mediaUrl: String?,
        backgroundColorHex: String?,
        privacy: StatusPrivacy
    ): Result<Unit> {
        var path = "statuses"
        return try {
            val uid = requireUserId()
            val ref = db.collection("statuses").document()
            path = ref.path
            val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L))
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "userName" to userName.take(80),
                "userPhotoUrl" to userPhotoUrl,
                "caption" to caption?.trim()?.take(InputValidator.MAX_STATUS_CAPTION),
                "mediaUrl" to mediaUrl,
                "type" to type.name,
                "backgroundColorHex" to backgroundColorHex,
                "privacy" to privacy.name,
                "viewerIds" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to expiresAt,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markStatusViewed(statusId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val path = "statuses/$statusId"
        return try {
            db.document(path).update(
                mapOf(
                    "viewerIds" to FieldValue.arrayUnion(uid),
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

    suspend fun deleteStatus(statusId: String): Result<Unit> {
        val path = "statuses/$statusId"
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

    suspend fun deleteExpiredOwnStatuses(): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.success(0)
        return try {
            val now = Timestamp.now()
            val snap = db.collection("statuses")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val expired = snap.documents.filter { doc ->
                val exp = doc.getTimestamp("expiresAt")
                exp != null && exp.seconds < now.seconds
            }
            if (expired.isNotEmpty()) {
                val batch = db.batch()
                expired.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(expired.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "statuses")
            Result.failure(e)
        }
    }
}
