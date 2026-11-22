package com.example.data.repository

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.BuildConfig
import com.example.domain.model.ContactAddedVia
import com.example.domain.model.PhoneLookupEntry
import com.example.domain.model.SavedContact
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserPrivateSettings
import com.example.domain.model.UserProfile
import com.example.security.InputValidator
import com.example.security.OperationType
import com.example.security.handleFirestoreError
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

sealed interface PhoneOtpEvent {
    data class CodeSent(
        val verificationId: String,
        val resendToken: PhoneAuthProvider.ForceResendingToken
    ) : PhoneOtpEvent
    data class AutoVerified(val credential: PhoneAuthCredential) : PhoneOtpEvent
    data class Failed(val message: String) : PhoneOtpEvent
}

class AuthAndUserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun currentUser(): FirebaseUser? = auth.currentUser

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")

    @SuppressLint("DiscouragedApi")
    private fun resolveWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId == 0) return null
        return runCatching { context.getString(resId).takeIf { it.isNotBlank() } }.getOrNull()
    }

    suspend fun attemptSilentGoogleSignIn(context: Context): Boolean {
        if (auth.currentUser != null) return true
        val clientId = resolveWebClientId(context) ?: return false
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(context, request).credential
            if (result is CustomCredential && result.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(result.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signInWithGoogleInteractive(activity: Activity): Result<FirebaseUser> {
        val clientId = resolveWebClientId(activity)
            ?: return Result.failure(IllegalStateException("Google Sign-In configuration missing."))
        return try {
            val credentialManager = CredentialManager.create(activity)
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            val credential = credentialManager.getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authResult = auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null)
                ).await()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Google Sign-In returned empty user."))
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type received."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    fun startPhoneNumberVerification(
        activity: Activity,
        phoneNumberE164: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onEvent: (PhoneOtpEvent) -> Unit
    ) {
        if (BuildConfig.DEBUG && isProbablyEmulator()) {
            // Emulators without an updated Play Store cannot bind Play Integrity (-9);
            // bypass Play Integrity and route directly to the reCAPTCHA verification fallback.
            try {
                auth.firebaseAuthSettings.forceRecaptchaFlowForTesting(true)
            } catch (e: Exception) {
                Log.w("PhoneAuth", "Could not configure emulator reCAPTCHA fallback: ${e.message}")
            }
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onEvent(PhoneOtpEvent.AutoVerified(credential))
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w("PhoneAuth", "Phone verification failed: ${e.message}", e)
                onEvent(PhoneOtpEvent.Failed(mapPhoneAuthError(e)))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                onEvent(PhoneOtpEvent.CodeSent(verificationId, token))
            }
        }

        try {
            val builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumberE164)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
            if (resendToken != null) {
                builder.setForceResendingToken(resendToken)
            }
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        } catch (e: Exception) {
            Log.w("PhoneAuth", "Failed to start phone verification: ${e.message}", e)
            onEvent(PhoneOtpEvent.Failed(mapPhoneAuthError(e)))
        }
    }

    private fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.PRODUCT.contains("emulator") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.HARDWARE.contains("goldfish")
    }

    private fun mapPhoneAuthError(e: Exception): String {
        val raw = e.message.orEmpty()
        val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
        return when {
            e is FirebaseAuthInvalidCredentialsException || code == "ERROR_INVALID_PHONE_NUMBER" ->
                "Invalid phone number format. Check your country code and number."
            e is FirebaseTooManyRequestsException || code == "ERROR_QUOTA_EXCEEDED" || code == "ERROR_TOO_MANY_REQUESTS" ->
                "Too many verification requests from this device or project. Please wait a bit or sign in with Google."
            e is FirebaseAuthMissingActivityForRecaptchaException ->
                "Unable to open verification challenge. Please try again or sign in with Google."
            code == "ERROR_OPERATION_NOT_ALLOWED" || raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                raw.contains("sign-in provider is disabled", ignoreCase = true) ->
                "Phone sign-in is not enabled yet in Firebase Console. Enable Phone Auth there, or sign in instantly with Google below."
            raw.contains("Integrity", ignoreCase = true) ||
                raw.contains("reCAPTCHA", ignoreCase = true) ||
                raw.contains("Recaptcha", ignoreCase = true) ||
                raw.contains("Play Store", ignoreCase = true) ||
                raw.contains("missing client identifier", ignoreCase = true) ||
                raw.contains("not authorized", ignoreCase = true) ->
                "Device verification (Play Integrity / reCAPTCHA) could not complete on this emulator or device. Ensure Play Store & Firebase Phone Auth / SHA-256 are configured, or sign in instantly with Google below."
            else -> e.localizedMessage
                ?: "Phone verification unavailable right now. Please try again or continue with Google Sign-In."
        }
    }

    suspend fun completePhoneCredential(credential: PhoneAuthCredential): Result<FirebaseUser> {
        return try {
            val current = auth.currentUser
            val user = if (current != null) {
                current.linkWithCredential(credential).await().user ?: current
            } else {
                auth.signInWithCredential(credential).await().user
                    ?: return Result.failure(IllegalStateException("Phone sign-in failed."))
            }
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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

    fun observePrivateSettings(userId: String): Flow<UserPrivateSettings?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Cannot read private settings of another user." }
        val path = "users/$userId/private/settings"
        emitAll(
            db.collection("users").document(userId)
                .collection("private").document("settings")
                .snapshots()
                .map { it.toObject(UserPrivateSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
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

    fun observeDiscoverableUsers(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .whereEqualTo("discoverable", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
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

    suspend fun getUserProfileOnce(userId: String): UserProfile? {
        return try {
            requireUserId()
            db.collection("users").document(userId).get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId")
            null
        }
    }

    suspend fun createOrUpdateProfile(
        displayName: String,
        about: String,
        photoUrl: String?,
        phoneNumber: String?,
        countryCode: String,
        discoverable: Boolean = true
    ): Result<Unit> {
        var path = "users"
        return try {
            val user = auth.currentUser
                ?: return Result.failure(IllegalStateException("User not signed in."))
            val uid = user.uid
            val cleanName = displayName.trim().take(InputValidator.MAX_DISPLAY_NAME)
            val cleanAbout = about.trim().take(InputValidator.MAX_ABOUT)
            InputValidator.validateDisplayName(cleanName)?.let {
                return Result.failure(IllegalArgumentException(it))
            }

            val profileRef = db.collection("users").document(uid)
            path = profileRef.path
            val existing = profileRef.get().await()
            if (existing.exists()) {
                val updateMap = InputValidator.nonNullFields(
                    "displayName" to cleanName,
                    "about" to cleanAbout,
                    "online" to true,
                    "discoverable" to discoverable,
                    "photoUrl" to photoUrl?.takeIf { it.isNotBlank() },
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profileRef.update(updateMap).await()
            } else {
                val createMap = InputValidator.nonNullFields(
                    "uid" to uid,
                    "displayName" to cleanName,
                    "photoUrl" to (photoUrl?.takeIf { it.isNotBlank() } ?: user.photoUrl?.toString()),
                    "about" to cleanAbout,
                    "online" to true,
                    "discoverable" to discoverable,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profileRef.set(createMap).await()
            }

            // Owner-only private PII & default settings
            val settingsRef = profileRef.collection("private").document("settings")
            path = settingsRef.path
            val existingSettings = settingsRef.get().await()
            if (existingSettings.exists()) {
                val settingsUpdate = InputValidator.nonNullFields(
                    "phoneNumber" to phoneNumber?.takeIf { it.isNotBlank() },
                    "countryCode" to countryCode,
                    "email" to user.email,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                settingsRef.update(settingsUpdate).await()
            } else {
                val settingsCreate = InputValidator.nonNullFields(
                    "uid" to uid,
                    "phoneNumber" to (phoneNumber?.takeIf { it.isNotBlank() } ?: user.phoneNumber),
                    "countryCode" to countryCode,
                    "email" to user.email,
                    "lastSeenVisible" to true,
                    "onlineVisible" to true,
                    "readReceiptsEnabled" to true,
                    "notifyMessages" to true,
                    "notifyGroups" to true,
                    "notifyCalls" to true,
                    "notifyStatus" to true,
                    "darkThemeMode" to ThemeMode.SYSTEM.name,
                    "blockedUserIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                settingsRef.set(settingsCreate).await()
            }

            // Publish exact-match phone lookup entry if a phone number is set
            val effectivePhone = phoneNumber?.takeIf { it.isNotBlank() } ?: user.phoneNumber
            val effectivePhoto = photoUrl?.takeIf { it.isNotBlank() }
                ?: existing.getString("photoUrl")
                ?: user.photoUrl?.toString()
            val lookupKey = effectivePhone?.let { InputValidator.phoneLookupKey(countryCode, it) }
            if (lookupKey != null) {
                try {
                    val lookupRef = db.collection("phoneLookup").document(lookupKey)
                    val lookupSnap = lookupRef.get().await()
                    if (!lookupSnap.exists()) {
                        lookupRef.set(
                            InputValidator.nonNullFields(
                                "uid" to uid,
                                "displayName" to cleanName,
                                "photoUrl" to effectivePhoto,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } else if (lookupSnap.getString("uid") == uid) {
                        lookupRef.update(
                            InputValidator.nonNullFields(
                                "displayName" to cleanName,
                                "photoUrl" to effectivePhoto,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("Auth", "Phone lookup index update skipped: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePresence(online: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "online" to online,
                        "lastSeen" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePrivateSettings(updates: Map<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in."))
        val path = "users/$uid/private/settings"
        return try {
            val ref = db.collection("users").document(uid).collection("private").document("settings")
            val snap = ref.get().await()
            if (snap.exists()) {
                val payload = updates.toMutableMap().apply {
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                ref.update(payload).await()
            } else {
                val payload = updates.toMutableMap().apply {
                    put("uid", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleBlockUser(targetUserId: String, block: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in."))
        val path = "users/$uid/private/settings"
        return try {
            val op = if (block) FieldValue.arrayUnion(targetUserId) else FieldValue.arrayRemove(targetUserId)
            db.collection("users").document(uid).collection("private").document("settings")
                .update(
                    mapOf(
                        "blockedUserIds" to op,
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

    suspend fun saveFcmToken(token: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return updatePrivateSettings(mapOf("fcmToken" to token))
    }

    suspend fun signOut(context: Context): Result<Unit> {
        return try {
            withContext(NonCancellable) {
                try {
                    updatePresence(false)
                } catch (_: Exception) {
                }
                try {
                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(context: Context): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Not signed in."))
        val uid = user.uid
        return try {
            withContext(NonCancellable) {
                try {
                    db.collection("users").document(uid).collection("private").document("settings").delete().await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, "users/$uid/private/settings")
                }
                try {
                    db.collection("users").document(uid).delete().await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, "users/$uid")
                }
                user.delete().await()
                try {
                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                } catch (_: Exception) {
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSavedContacts(): Flow<List<SavedContact>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/contacts"
        emitAll(
            db.collection("users").document(uid).collection("contacts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
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

    suspend fun findUserByPhone(dialCode: String, localOrFullNumber: String): Result<UserProfile?> {
        return try {
            val uid = requireUserId()
            val primaryKey = InputValidator.phoneLookupKey(dialCode, localOrFullNumber)
                ?: return Result.failure(IllegalArgumentException("Enter a valid phone number."))
            val rawDigits = localOrFullNumber.filter { it.isDigit() }
            val keysToTry = listOfNotNull(
                primaryKey,
                rawDigits.takeIf { it.length in 6..15 && it != primaryKey }
            )
            var foundEntry: PhoneLookupEntry? = null
            for (key in keysToTry) {
                val snap = db.collection("phoneLookup").document(key).get().await()
                if (snap.exists()) {
                    foundEntry = snap.toObject(PhoneLookupEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (foundEntry != null) break
                }
            }
            val entry = foundEntry ?: return Result.success(null)
            if (entry.uid == uid) {
                return Result.failure(IllegalArgumentException("That phone number belongs to your own account."))
            }
            val profile = getUserProfileOnce(entry.uid) ?: UserProfile(
                id = entry.uid,
                uid = entry.uid,
                displayName = entry.displayName,
                photoUrl = entry.photoUrl
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "phoneLookup")
            Result.failure(e)
        }
    }

    suspend fun resolveScannedContact(rawQr: String): Result<UserProfile?> {
        return try {
            val uid = requireUserId()
            val target = InputValidator.parseContactQrPayload(rawQr)
                ?: return Result.failure(IllegalArgumentException("QR code is not a valid Manoj & Sonali contact."))
            when (target) {
                is InputValidator.ScannedContactTarget.ByUid -> {
                    if (target.uid == uid) {
                        return Result.failure(IllegalArgumentException("This is your own QR code."))
                    }
                    val profile = getUserProfileOnce(target.uid)
                    Result.success(profile)
                }
                is InputValidator.ScannedContactTarget.ByPhone -> {
                    findUserByPhone("", target.rawPhone)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveContact(
        contact: UserProfile,
        phoneNumber: String?,
        via: ContactAddedVia
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            if (contact.uid.isBlank() || contact.uid == uid) {
                return Result.failure(IllegalArgumentException("Cannot add yourself as a contact."))
            }
            val ref = db.collection("users").document(uid)
                .collection("contacts").document(contact.uid)
            path = ref.path
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.update(
                    InputValidator.nonNullFields(
                        "displayName" to contact.displayName.take(InputValidator.MAX_DISPLAY_NAME),
                        "photoUrl" to contact.photoUrl,
                        "about" to contact.about.take(InputValidator.MAX_ABOUT),
                        "phoneNumber" to phoneNumber?.takeIf { it.isNotBlank() }?.take(32),
                        "addedVia" to via.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    InputValidator.nonNullFields(
                        "contactUid" to contact.uid,
                        "displayName" to contact.displayName.take(InputValidator.MAX_DISPLAY_NAME),
                        "photoUrl" to contact.photoUrl,
                        "about" to contact.about.take(InputValidator.MAX_ABOUT),
                        "phoneNumber" to phoneNumber?.takeIf { it.isNotBlank() }?.take(32),
                        "addedVia" to via.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeContact(contactUid: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in."))
        val path = "users/$uid/contacts/$contactUid"
        return try {
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
