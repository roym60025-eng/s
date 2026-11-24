package com.example.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ThemeMode
import com.example.presentation.components.EmptyIllustrationState
import com.example.presentation.components.UserAvatar
import com.example.profile.ProfileSettingsViewModel

import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import com.example.presentation.components.ErrorBannerCard

@Composable
fun ProfileTabContent(
    viewModel: ProfileSettingsViewModel,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMyQr: () -> Unit,
    onOpenAddContact: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val settings by viewModel.privateSettings.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isErrorStatus by viewModel.isErrorStatus.collectAsStateWithLifecycle()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.uploadNewAvatar(uri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uploadProgress != null) {
            LinearProgressIndicator(progress = { uploadProgress ?: 0f }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (statusMessage != null) {
            if (isErrorStatus) {
                ErrorBannerCard(
                    message = statusMessage.orEmpty(),
                    onRetry = viewModel::clearStatusMessage
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::clearStatusMessage) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .clickable(enabled = uploadProgress == null) {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                .testTag("profile_avatar_container")
        ) {
            UserAvatar(
                name = profile?.displayName ?: "Member",
                photoUrl = profile?.photoUrl,
                size = 104.dp,
                isOnline = true
            )
            FilledTonalIconButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = uploadProgress == null,
                modifier = Modifier.testTag("change_profile_photo_button")
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Change avatar")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        TextButton(
            onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            enabled = uploadProgress == null
        ) {
            Text(if (uploadProgress != null) "Updating photo..." else "Change profile photo")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = profile?.displayName ?: "Manoj & Sonali User",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = settings?.phoneNumber ?: settings?.email ?: "Verified Member",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(profile?.about ?: "Available on Manoj & Sonali", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onOpenMyQr,
                modifier = Modifier.weight(1f).height(50.dp).testTag("profile_my_qr_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("My QR Code")
            }
            FilledTonalButton(
                onClick = onOpenAddContact,
                modifier = Modifier.weight(1f).height(50.dp).testTag("profile_add_contact_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Contact")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("edit_profile_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("open_settings_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Privacy, Notifications & Theme")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { viewModel.signOut(context) },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("profile_logout_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileSettingsViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val settings by viewModel.privateSettings.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isErrorStatus by viewModel.isErrorStatus.collectAsStateWithLifecycle()

    var name by remember(profile) { mutableStateOf(profile?.displayName.orEmpty()) }
    var about by remember(profile) { mutableStateOf(profile?.about.orEmpty()) }
    var phone by remember(settings) { mutableStateOf(settings?.phoneNumber.orEmpty()) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.uploadNewAvatar(uri) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Edit Profile") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uploadProgress != null) {
                LinearProgressIndicator(progress = { uploadProgress ?: 0f }, modifier = Modifier.fillMaxWidth())
            }
            if (statusMessage != null && isErrorStatus) {
                ErrorBannerCard(
                    message = statusMessage.orEmpty(),
                    onRetry = viewModel::clearStatusMessage
                )
            }

            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable(enabled = uploadProgress == null) {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            ) {
                UserAvatar(
                    name = name.ifBlank { profile?.displayName ?: "Member" },
                    photoUrl = profile?.photoUrl,
                    size = 96.dp
                )
                FilledTonalIconButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = uploadProgress == null
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Change profile photo")
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text("About") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveProfileEdits(name, about, phone, onBack) },
                enabled = name.isNotBlank() && uploadProgress == null,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Save Changes")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ProfileSettingsViewModel,
    onBack: () -> Unit,
    onOpenBlocked: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.privateSettings.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                Text("Privacy", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            item {
                SettingToggleRow(
                    title = "Last Seen Visibility",
                    subtitle = "Allow contacts to see when you were last active",
                    checked = settings?.lastSeenVisible ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("lastSeenVisible", it) }
                )
            }
            item {
                SettingToggleRow(
                    title = "Online Status Visibility",
                    subtitle = "Show real-time online indicator",
                    checked = settings?.onlineVisible ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("onlineVisible", it) }
                )
            }
            item {
                SettingToggleRow(
                    title = "Read Receipts",
                    subtitle = "Send and receive blue read checkmarks",
                    checked = settings?.readReceiptsEnabled ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("readReceiptsEnabled", it) }
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenBlocked)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Block, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Blocked Contacts", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${settings?.blockedUserIds?.size ?: 0} blocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                Text("Notifications", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            item {
                SettingToggleRow(
                    title = "Message Notifications",
                    subtitle = "Direct 1-to-1 chat alerts",
                    checked = settings?.notifyMessages ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("notifyMessages", it) }
                )
            }
            item {
                SettingToggleRow(
                    title = "Group Notifications",
                    subtitle = "Group messages and mentions",
                    checked = settings?.notifyGroups ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("notifyGroups", it) }
                )
            }
            item {
                SettingToggleRow(
                    title = "Call Notifications",
                    subtitle = "Incoming voice & video ring alerts",
                    checked = settings?.notifyCalls ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("notifyCalls", it) }
                )
            }
            item {
                SettingToggleRow(
                    title = "Status Updates",
                    subtitle = "24-hour status alerts",
                    checked = settings?.notifyStatus ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("notifyStatus", it) }
                )
            }

            item { HorizontalDivider() }

            item {
                OutlinedButton(
                    onClick = { viewModel.signOut(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out")
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("delete_account_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account?") },
            text = { Text("This permanently deletes your Manoj & Sonali profile, private settings, and authentication credentials.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccount(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedContactsScreen(
    viewModel: ProfileSettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.privateSettings.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val blockedIds = settings?.blockedUserIds.orEmpty()
    val usersMap = remember(allUsers) { allUsers.associateBy { it.uid } }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Blocked Contacts") }
            )
        }
    ) { innerPadding ->
        if (blockedIds.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                EmptyIllustrationState(
                    title = "No blocked contacts",
                    subtitle = "Contacts you block will no longer appear in your chat feed."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(blockedIds, key = { it }) { uid ->
                    val user = usersMap[uid]
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(user?.displayName ?: "User", user?.photoUrl, size = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(user?.displayName ?: uid, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.unblockUser(uid) }) {
                            Text("Unblock")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
