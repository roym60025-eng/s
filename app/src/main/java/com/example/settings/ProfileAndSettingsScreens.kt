package com.example.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ModalBottomSheet
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.ThemeMode
import com.example.presentation.components.EmptyIllustrationState
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.UserAvatar
import com.example.profile.ProfileSettingsViewModel
import com.example.ui.theme.AppThemeTokens
import com.example.ui.theme.themePaletteFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileSettingsViewModel,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBlocked: () -> Unit,
    onOpenMyQr: () -> Unit,
    onOpenAddContact: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val settings by viewModel.privateSettings.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val appThemeStyle by viewModel.appThemeStyle.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isErrorStatus by viewModel.isErrorStatus.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.uploadNewAvatar(uri) }

    val palette = AppThemeTokens.current
    val headerColor = palette.headerColor

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Showcase #10 Curved Forest Green Hero Header + Overlapping Avatar with Camera Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(178.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w, 0f)
                        lineTo(w, h * 0.72f)
                        quadraticTo(w * 0.5f, h * 1.12f, 0f, h * 0.72f)
                        close()
                    }
                    drawPath(path = path, color = headerColor)
                }

                // Top bar navigation inside curved green arch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Profile options", tint = Color.White)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit Profile") },
                                onClick = { showMenu = false; onEditProfile() }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = { showMenu = false; onOpenSettings() }
                            )
                            DropdownMenuItem(
                                text = { Text("Log Out") },
                                onClick = { showMenu = false; viewModel.signOut(context) }
                            )
                        }
                    }
                }

                // Centered Avatar straddling bottom of curved green arch
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clickable(enabled = uploadProgress == null) {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .testTag("profile_avatar_container")
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .padding(2.dp)
                    ) {
                        UserAvatar(
                            name = profile?.displayName ?: "Member",
                            photoUrl = profile?.photoUrl,
                            size = 126.dp,
                            isOnline = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2C34))
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .clickable(enabled = uploadProgress == null) {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .testTag("change_profile_photo_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Change avatar",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (uploadProgress != null) {
                LinearProgressIndicator(
                    progress = { uploadProgress ?: 0f },
                    color = palette.accentColor,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }

            if (statusMessage != null) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    if (isErrorStatus) {
                        ErrorBannerCard(
                            message = statusMessage.orEmpty(),
                            onRetry = viewModel::clearStatusMessage
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
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
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = profile?.displayName ?: "Manoj & Sonali User",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Online",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.accentColor
            )
            val contactLine = settings?.phoneNumber ?: settings?.email
            if (!contactLine.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contactLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // About Section matching Showcase #10
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditProfile)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .testTag("edit_profile_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit profile",
                        tint = palette.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile?.about ?: "Keep Smiling... 😊",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )

            // Action Rows matching Showcase #10 clean list style
            ProfileMenuRow(
                icon = Icons.Default.QrCode2,
                title = "My QR Code",
                onClick = onOpenMyQr,
                modifier = Modifier.testTag("profile_my_qr_button")
            )
            ProfileMenuRow(
                icon = Icons.Default.PersonAdd,
                title = "Add Contact",
                onClick = onOpenAddContact,
                modifier = Modifier.testTag("profile_add_contact_button")
            )
            ProfileMenuRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                onClick = onOpenSettings,
                modifier = Modifier.testTag("open_settings_button")
            )
            ProfileMenuRow(
                icon = Icons.Default.Lock,
                title = "Privacy",
                trailingText = "${settings?.blockedUserIds?.size ?: 0} blocked",
                onClick = onOpenBlocked
            )
            ProfileMenuRow(
                icon = Icons.Default.Palette,
                title = "Theme & Appearance",
                trailingText = appThemeStyle.displayName,
                trailingColorBadge = palette.accentColor,
                onClick = { showThemeSheet = true },
                modifier = Modifier.testTag("profile_theme_button")
            )
            ProfileMenuRow(
                icon = Icons.Default.Settings,
                title = "App Settings",
                onClick = onOpenSettings
            )
            ProfileMenuRow(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = "Log Out",
                iconTint = MaterialTheme.colorScheme.error,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { viewModel.signOut(context) },
                modifier = Modifier.testTag("profile_logout_button")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                ThemeStudioSection(
                    selectedStyle = appThemeStyle,
                    selectedMode = themeMode,
                    onSelectStyle = viewModel::setAppThemeStyle,
                    onSelectMode = viewModel::setThemeMode
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        showThemeSheet = false
                        onOpenSettings()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("More Privacy & Notification Settings")
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    trailingColorBadge: Color? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = titleColor,
            modifier = Modifier.weight(1f)
        )
        if (!trailingText.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailingColorBadge != null) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(trailingColorBadge)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

    val palette = AppThemeTokens.current
    val headerColor = palette.headerColor

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.uploadNewAvatar(uri) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerColor,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                ),
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
                LinearProgressIndicator(progress = { uploadProgress ?: 0f }, color = palette.accentColor, modifier = Modifier.fillMaxWidth())
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
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.buttonColor)
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
    val appThemeStyle by viewModel.appThemeStyle.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val palette = AppThemeTokens.current
    val headerColor = palette.headerColor

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerColor,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Themes & Settings") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ThemeStudioSection(
                    selectedStyle = appThemeStyle,
                    selectedMode = themeMode,
                    onSelectStyle = viewModel::setAppThemeStyle,
                    onSelectMode = viewModel::setThemeMode
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp)) }

            item {
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accentColor,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                SettingToggleRow(
                    title = "Last Seen Visibility",
                    subtitle = "Allow contacts to see when you were last active",
                    checked = settings?.lastSeenVisible ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("lastSeenVisible", it) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                SettingToggleRow(
                    title = "Online Status Visibility",
                    subtitle = "Show real-time online indicator",
                    checked = settings?.onlineVisible ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("onlineVisible", it) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                SettingToggleRow(
                    title = "Read Receipts",
                    subtitle = "Send and receive blue read checkmarks",
                    checked = settings?.readReceiptsEnabled ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("readReceiptsEnabled", it) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenBlocked)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
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

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp)) }

            item {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accentColor,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                SettingToggleRow(
                    title = "Message Notifications",
                    subtitle = "Direct 1-to-1 chat alerts",
                    checked = settings?.notifyMessages ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("notifyMessages", it) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                SettingToggleRow(
                    title = "Group Notifications",
                    subtitle = "Group messages and mentions",
                    checked = settings?.notifyGroups ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("notifyGroups", it) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                SettingToggleRow(
                    title = "Call Notifications",
                    subtitle = "Incoming voice & video ring alerts",
                    checked = settings?.notifyCalls ?: true,
                    onCheckedChange = { viewModel.updateSettingToggle("notifyCalls", it) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp)) }

            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
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

    val palette = AppThemeTokens.current
    val headerColor = palette.headerColor

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerColor,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                ),
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
private fun ThemeStudioSection(
    selectedStyle: AppThemeStyle,
    selectedMode: ThemeMode,
    onSelectStyle: (AppThemeStyle) -> Unit,
    onSelectMode: (ThemeMode) -> Unit
) {
    val activePalette = AppThemeTokens.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Theme Studio",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = activePalette.accentColor
                )
                Text(
                    text = "Choose a color theme and Light or Dark mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val selected = selectedMode == mode
                val icon = when (mode) {
                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    ThemeMode.LIGHT -> Icons.Default.LightMode
                    ThemeMode.DARK -> Icons.Default.DarkMode
                }
                FilterChip(
                    selected = selected,
                    onClick = { onSelectMode(mode) },
                    leadingIcon = {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(AppThemeStyle.entries, key = { it.name }) { style ->
                val isSelected = selectedStyle == style
                val preview = themePaletteFor(style, darkTheme = activePalette.isDark)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) preview.accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    tonalElevation = if (isSelected) 4.dp else 1.dp,
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelectStyle(style) }
                        .testTag("theme_card_${style.name.lowercase()}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Mini-phone chat preview inside theme card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(preview.chatWallpaperColor)
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Mini Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .background(preview.headerColor)
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(44.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color.White.copy(alpha = 0.85f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.8f))
                                    )
                                }

                                // Mini Incoming & Outgoing Chat Bubbles
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = preview.incomingBubbleColor,
                                        shadowElevation = 0.5.dp,
                                        modifier = Modifier.width(68.dp).height(18.dp)
                                    ) {}
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = preview.outgoingBubbleColor,
                                        shadowElevation = 0.5.dp,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .width(74.dp)
                                            .height(20.dp)
                                    ) {}
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = preview.incomingBubbleColor,
                                        shadowElevation = 0.5.dp,
                                        modifier = Modifier.width(52.dp).height(16.dp)
                                    ) {}
                                }
                            }

                            // Mini FAB & Selected Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(preview.accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = style.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
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
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
