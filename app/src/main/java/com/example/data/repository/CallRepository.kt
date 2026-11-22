package com.example.data.repository

import com.example.domain.model.CallSession
import com.example.domain.model.CallStatus
import com.example.domain.model.CallType
import com.example.security.OperationType
import com.example.security.handleFirestoreError
import com.google.firebase.Firebase
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CallRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing calls.")

    fun observeCallHistory(): Flow<List<CallSession>> = flow {
        val uid = requireUserId()
        val path = "calls"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.startedAt?.toDate()?.time ?: 0L }
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

    fun observeCall(callId: String): Flow<CallSession?> = flow {
        val uid = requireUserId()
        val path = "calls/$callId"
        emitAll(
            db.document(path)
                .snapshots()
                .map { it.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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

    suspend fun initiateCall(
        callerName: String,
        callerPhotoUrl: String?,
        receiverId: String,
        receiverName: String,
        receiverPhotoUrl: String?,
        callType: CallType
    ): Result<String> {
        var path = "calls"
        return try {
            val uid = requireUserId()
            val ref = db.collection("calls").document()
            path = ref.path
            val payload = mutableMapOf<String, Any?>(
                "callerId" to uid,
                "callerName" to callerName.take(80),
                "callerPhotoUrl" to callerPhotoUrl,
                "receiverId" to receiverId,
                "receiverName" to receiverName.take(80),
                "receiverPhotoUrl" to receiverPhotoUrl,
                "participantIds" to listOf(uid, receiverId),
                "callType" to callType.name,
                "status" to CallStatus.RINGING.name,
                "callerCandidates" to emptyList<String>(),
                "receiverCandidates" to emptyList<String>(),
                "startedAt" to FieldValue.serverTimestamp(),
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

    suspend fun publishOfferSdp(callId: String, offerSdp: String): Result<Unit> =
        updateCall(callId, mapOf("offerSdp" to offerSdp.take(20000)))

    suspend fun acceptCallWithAnswer(callId: String, answerSdp: String): Result<Unit> =
        updateCall(
            callId,
            mapOf(
                "status" to CallStatus.ACCEPTED.name,
                "answerSdp" to answerSdp.take(20000)
            )
        )

    suspend fun addIceCandidate(callId: String, isCaller: Boolean, candidateSerialized: String): Result<Unit> {
        val field = if (isCaller) "callerCandidates" else "receiverCandidates"
        return updateCall(callId, mapOf(field to FieldValue.arrayUnion(candidateSerialized)))
    }

    suspend fun updateCallStatus(callId: String, status: CallStatus): Result<Unit> {
        val updates = mutableMapOf<String, Any>("status" to status.name)
        if (status == CallStatus.ENDED || status == CallStatus.REJECTED || status == CallStatus.MISSED) {
            updates["endedAt"] = FieldValue.serverTimestamp()
        }
        return updateCall(callId, updates)
    }

    private suspend fun updateCall(callId: String, fields: Map<String, Any>): Result<Unit> {
        val path = "calls/$callId"
        return try {
            requireUserId()
            val payload = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.document(path).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
