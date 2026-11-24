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
import com.example.data.repository.StatusRepository
import com.example.notifications.IncomingNotificationMonitor
import com.example.security.KeystoreManager
import com.example.utils.VoiceRecorderPlayer
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(appContext: Context) {
    private val databaseId: String = appContext.getString(R.string.firestore_database_id)
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)
    val auth = Firebase.auth

    val keystoreManager = KeystoreManager()
    val preferencesDataStore = PreferencesDataStore(appContext, keystoreManager)
    val storageMediaRepository = StorageMediaRepository(appContext)
    val voiceRecorderPlayer = VoiceRecorderPlayer(appContext)

    val authAndUserRepository = AuthAndUserRepository(firestore, auth)
    val chatRepository = ChatRepository(firestore, auth)
    val groupRepository = GroupRepository(firestore, auth)
    val statusRepository = StatusRepository(firestore, auth)
    val callRepository = CallRepository(firestore, auth)
    val webRtcCallManager = WebRtcCallManager(appContext, callRepository)
    val notificationMonitor = IncomingNotificationMonitor(
        appContext = appContext,
        authRepository = authAndUserRepository,
        chatRepository = chatRepository,
        groupRepository = groupRepository,
        callRepository = callRepository,
        preferencesDataStore = preferencesDataStore
    )
}
