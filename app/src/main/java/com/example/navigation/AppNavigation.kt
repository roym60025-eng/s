package com.example.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.authentication.AuthFlowRoot
import com.example.authentication.AuthViewModel
import com.example.calls.ActiveCallScreen
import com.example.calls.CallHistoryTabContent
import com.example.calls.CallsViewModel
import com.example.calls.IncomingCallBanner
import com.example.di.AppContainer
import com.example.domain.model.CallType
import com.example.groups.CreateGroupScreen
import com.example.groups.GroupChatScreen
import com.example.groups.GroupInfoScreen
import com.example.groups.GroupsViewModel
import com.example.messaging.ChatDetailScreen
import com.example.messaging.ChatDetailViewModel
import com.example.presentation.components.LoadingStateBox
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.profile.ProfileSettingsViewModel
import com.example.settings.BlockedContactsScreen
import com.example.settings.EditProfileScreen
import com.example.settings.ProfileTabContent
import com.example.settings.SettingsScreen
import com.example.status.CreateStatusScreen
import com.example.status.StatusViewModel
import com.example.status.StatusViewerScreen
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object Routes {
    const val HOME = "home"
    const val CHAT_DETAIL = "chat_detail/{chatId}"
    const val CREATE_GROUP = "create_group"
    const val GROUP_CHAT = "group_chat/{groupId}"
    const val GROUP_INFO = "group_info/{groupId}"
    const val CREATE_STATUS = "create_status"
    const val VIEW_STATUS = "view_status/{userId}"
    const val ACTIVE_CALL = "active_call/{callId}"
    const val EDIT_PROFILE = "edit_profile"
    const val SETTINGS = "settings"
    const val BLOCKED_CONTACTS = "blocked_contacts"

    fun chatDetail(chatId: String) = "chat_detail/$chatId"
    fun groupChat(groupId: String) = "group_chat/$groupId"
    fun groupInfo(groupId: String) = "group_info/$groupId"
    fun viewStatus(userId: String) = "view_status/$userId"
    fun activeCall(callId: String) = "active_call/$callId"
}

@Composable
fun AppNavigation(container: AppContainer) {
    val context = LocalContext.current
    val currentUser by container.authAndUserRepository.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = container.authAndUserRepository.currentUser())

    var silentAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!silentAttempted) {
            silentAttempted = true
            container.authAndUserRepository.attemptSilentGoogleSignIn(context)
        }
    }

    val user = currentUser
    if (user == null) {
        val authViewModel: AuthViewModel = viewModel(
            key = "auth_flow",
            factory = viewModelFactory {
                initializer {
                    AuthViewModel(
                        authRepository = container.authAndUserRepository,
                        storageRepository = container.storageMediaRepository,
                        preferencesDataStore = container.preferencesDataStore
                    )
                }
            }
        )
        AuthFlowRoot(
            viewModel = authViewModel,
            startAtProfileSetup = false,
            onAuthenticated = {}
        )
    } else {
        key(user.uid) {
            AuthenticatedShell(
                currentUserId = user.uid,
                container = container
            )
        }
    }
}

@Composable
private fun AuthenticatedShell(
    currentUserId: String,
    container: AppContainer
) {
    val context = LocalContext.current
    var profileChecked by remember { mutableStateOf(false) }
    var needsProfileSetup by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(currentUserId) {
        val existing = container.authAndUserRepository.getUserProfileOnce(currentUserId)
        needsProfileSetup = existing == null || existing.displayName.isBlank()
        profileChecked = true
        if (!needsProfileSetup) {
            container.authAndUserRepository.updatePresence(true)
        }
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (!token.isNullOrBlank()) {
                container.authAndUserRepository.saveFcmToken(token)
            }
        } catch (_: Exception) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!profileChecked) {
        LoadingStateBox("Preparing your workspace...")
        return
    }

    if (needsProfileSetup) {
        val authViewModel: AuthViewModel = viewModel(
            key = "profile_setup_$currentUserId",
            factory = viewModelFactory {
                initializer {
                    AuthViewModel(
                        authRepository = container.authAndUserRepository,
                        storageRepository = container.storageMediaRepository,
                        preferencesDataStore = container.preferencesDataStore
                    )
                }
            }
        )
        AuthFlowRoot(
            viewModel = authViewModel,
            startAtProfileSetup = true,
            onAuthenticated = { needsProfileSetup = false }
        )
        return
    }

    val navController = rememberNavController()

    val homeViewModel: HomeViewModel = viewModel(
        key = "home_$currentUserId",
        factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    currentUserId = currentUserId,
                    authRepository = container.authAndUserRepository,
                    chatRepository = container.chatRepository,
                    groupRepository = container.groupRepository,
                    statusRepository = container.statusRepository,
                    callRepository = container.callRepository
                )
            }
        }
    )

    val groupsViewModel: GroupsViewModel = viewModel(
        key = "groups_$currentUserId",
        factory = viewModelFactory {
            initializer {
                GroupsViewModel(
                    currentUserId = currentUserId,
                    authRepository = container.authAndUserRepository,
                    groupRepository = container.groupRepository,
                    storageRepository = container.storageMediaRepository
                )
            }
        }
    )

    val statusViewModel: StatusViewModel = viewModel(
        key = "status_$currentUserId",
        factory = viewModelFactory {
            initializer {
                StatusViewModel(
                    currentUserId = currentUserId,
                    authRepository = container.authAndUserRepository,
                    statusRepository = container.statusRepository,
                    storageRepository = container.storageMediaRepository
                )
            }
        }
    )

    val callsViewModel: CallsViewModel = viewModel(
        key = "calls_$currentUserId",
        factory = viewModelFactory {
            initializer {
                CallsViewModel(
                    currentUserId = currentUserId,
                    authRepository = container.authAndUserRepository,
                    callRepository = container.callRepository,
                    webRtcCallManager = container.webRtcCallManager
                )
            }
        }
    )

    val profileSettingsViewModel: ProfileSettingsViewModel = viewModel(
        key = "profile_$currentUserId",
        factory = viewModelFactory {
            initializer {
                ProfileSettingsViewModel(
                    currentUserId = currentUserId,
                    authRepository = container.authAndUserRepository,
                    storageRepository = container.storageMediaRepository,
                    preferencesDataStore = container.preferencesDataStore
                )
            }
        }
    )

    val incomingCall by homeViewModel.incomingCall.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onOpenChat = { chatId -> navController.navigate(Routes.chatDetail(chatId)) },
                    onOpenGroup = { groupId -> navController.navigate(Routes.groupChat(groupId)) },
                    onCreateGroup = { navController.navigate(Routes.CREATE_GROUP) },
                    onCreateStatus = { navController.navigate(Routes.CREATE_STATUS) },
                    onViewUserStatuses = { uid -> navController.navigate(Routes.viewStatus(uid)) },
                    onStartVoiceCall = { peer ->
                        callsViewModel.startOutgoingCall(peer, CallType.VOICE) { callId ->
                            navController.navigate(Routes.activeCall(callId))
                        }
                    },
                    onStartVideoCall = { peer ->
                        callsViewModel.startOutgoingCall(peer, CallType.VIDEO) { callId ->
                            navController.navigate(Routes.activeCall(callId))
                        }
                    },
                    callsTabContent = { callsState, usersMap ->
                        CallHistoryTabContent(
                            currentUserId = currentUserId,
                            callsState = callsState,
                            usersMap = usersMap,
                            onStartVoiceCall = { peer ->
                                callsViewModel.startOutgoingCall(peer, CallType.VOICE) { callId ->
                                    navController.navigate(Routes.activeCall(callId))
                                }
                            },
                            onStartVideoCall = { peer ->
                                callsViewModel.startOutgoingCall(peer, CallType.VIDEO) { callId ->
                                    navController.navigate(Routes.activeCall(callId))
                                }
                            }
                        )
                    },
                    profileTabContent = {
                        ProfileTabContent(
                            viewModel = profileSettingsViewModel,
                            onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                        )
                    }
                )
            }

            composable(
                route = Routes.CHAT_DETAIL,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { entry ->
                val chatId = entry.arguments?.getString("chatId").orEmpty()
                val chatVm: ChatDetailViewModel = viewModel(
                    key = "chat_${currentUserId}_$chatId",
                    factory = viewModelFactory {
                        initializer {
                            ChatDetailViewModel(
                                chatId = chatId,
                                currentUserId = currentUserId,
                                authRepository = container.authAndUserRepository,
                                chatRepository = container.chatRepository,
                                storageRepository = container.storageMediaRepository,
                                voiceRecorderPlayer = container.voiceRecorderPlayer
                            )
                        }
                    }
                )
                ChatDetailScreen(
                    viewModel = chatVm,
                    onBack = { navController.popBackStack() },
                    onStartVoiceCall = { peer ->
                        callsViewModel.startOutgoingCall(peer, CallType.VOICE) { callId ->
                            navController.navigate(Routes.activeCall(callId))
                        }
                    },
                    onStartVideoCall = { peer ->
                        callsViewModel.startOutgoingCall(peer, CallType.VIDEO) { callId ->
                            navController.navigate(Routes.activeCall(callId))
                        }
                    }
                )
            }

            composable(Routes.CREATE_GROUP) {
                CreateGroupScreen(
                    viewModel = groupsViewModel,
                    onBack = { navController.popBackStack() },
                    onGroupCreated = { groupId ->
                        navController.popBackStack()
                        navController.navigate(Routes.groupChat(groupId))
                    }
                )
            }

            composable(
                route = Routes.GROUP_CHAT,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { entry ->
                val groupId = entry.arguments?.getString("groupId").orEmpty()
                GroupChatScreen(
                    groupId = groupId,
                    viewModel = groupsViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenInfo = { navController.navigate(Routes.groupInfo(groupId)) }
                )
            }

            composable(
                route = Routes.GROUP_INFO,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { entry ->
                val groupId = entry.arguments?.getString("groupId").orEmpty()
                GroupInfoScreen(
                    groupId = groupId,
                    viewModel = groupsViewModel,
                    onBack = { navController.popBackStack() },
                    onExitedGroup = {
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    }
                )
            }

            composable(Routes.CREATE_STATUS) {
                CreateStatusScreen(
                    viewModel = statusViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.VIEW_STATUS,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { entry ->
                val targetUserId = entry.arguments?.getString("userId").orEmpty()
                StatusViewerScreen(
                    targetUserId = targetUserId,
                    viewModel = statusViewModel,
                    onClose = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ACTIVE_CALL,
                arguments = listOf(navArgument("callId") { type = NavType.StringType })
            ) { entry ->
                val callId = entry.arguments?.getString("callId").orEmpty()
                ActiveCallScreen(
                    callId = callId,
                    viewModel = callsViewModel,
                    onCallClosed = { navController.popBackStack() }
                )
            }

            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    viewModel = profileSettingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = profileSettingsViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenBlocked = { navController.navigate(Routes.BLOCKED_CONTACTS) }
                )
            }

            composable(Routes.BLOCKED_CONTACTS) {
                BlockedContactsScreen(
                    viewModel = profileSettingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        incomingCall?.let { call ->
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != Routes.ACTIVE_CALL) {
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    IncomingCallBanner(
                        call = call,
                        onAccept = {
                            navController.navigate(Routes.activeCall(call.id))
                        },
                        onDecline = {
                            callsViewModel.rejectCall(call.id)
                        }
                    )
                }
            }
        }
    }
}
