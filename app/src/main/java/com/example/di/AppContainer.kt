package com.example.di

import android.content.Context
import com.example.R
import com.example.calls.WebRtcCallManager
import com.example.data.local.PreferencesDataStore
import com.example.data.remote.StorageMediaRepository
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.GroupRepository
import com.example.notifications.IncomingNotificationMonitor
import com.example.photos.GooglePhotosRepository
import com.example.security.KeystoreManager
import com.example.utils.VoiceRecorderPlayer
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(appContext: Context) {
    private val context = appContext.applicationContext
    private val databaseId: String by lazy { context.getString(R.string.firestore_database_id) }

    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance(databaseId) }
    val auth by lazy { Firebase.auth }

    val keystoreManager: KeystoreManager by lazy { KeystoreManager() }
    val preferencesDataStore: PreferencesDataStore by lazy {
        PreferencesDataStore(context, keystoreManager)
    }
    val storageMediaRepository: StorageMediaRepository by lazy {
        StorageMediaRepository(context)
    }
    val voiceRecorderPlayer: VoiceRecorderPlayer by lazy {
        VoiceRecorderPlayer(context)
    }

    val authAndUserRepository: AuthAndUserRepository by lazy {
        AuthAndUserRepository(firestore, auth)
    }
    val chatRepository: ChatRepository by lazy {
        ChatRepository(firestore, auth)
    }
    val groupRepository: GroupRepository by lazy {
        GroupRepository(firestore, auth)
    }
    val googlePhotosRepository: GooglePhotosRepository by lazy {
        GooglePhotosRepository()
    }
    val callRepository: CallRepository by lazy {
        CallRepository(firestore, auth)
    }
    val webRtcCallManager: WebRtcCallManager by lazy {
        WebRtcCallManager(context, callRepository)
    }
    val notificationMonitor: IncomingNotificationMonitor by lazy {
        IncomingNotificationMonitor(
            appContext = context,
            authRepository = authAndUserRepository,
            chatRepository = chatRepository,
            groupRepository = groupRepository,
            callRepository = callRepository,
            preferencesDataStore = preferencesDataStore
        )
    }
}
