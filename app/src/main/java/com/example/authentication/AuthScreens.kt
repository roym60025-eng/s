package com.example.authentication

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.presentation.components.BrandLogoMark
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.GoogleGLogo
import com.example.ui.theme.AppThemeTokens
import com.example.utils.CountryCode
import com.example.utils.CountryCodes

@Composable
fun AuthFlowRoot(
    viewModel: AuthViewModel,
    startAtProfileSetup: Boolean = false,
    onAuthenticated: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(startAtProfileSetup) {
        if (startAtProfileSetup) {
            viewModel.prefillFromCurrentUser()
            viewModel.navigateTo(AuthStep.CREATE_PROFILE)
        }
    }

    AnimatedContent(targetState = state.step, label = "auth_step") { step ->
        when (step) {
            AuthStep.WELCOME,
            AuthStep.PHONE_INPUT,
            AuthStep.OTP_VERIFY -> WelcomeScreen(
                state = state,
                onGoogleSignIn = { activity ->
                    viewModel.signInWithGoogle(activity, onSignedInAndReady = onAuthenticated)
                }
            )

            AuthStep.CREATE_PROFILE -> {
                CreateProfileScreen(
                    state = state,
                    onDisplayNameChanged = viewModel::onDisplayNameChanged,
                    onAboutChanged = viewModel::onAboutChanged,
                    onPhoneChanged = viewModel::onPhoneNumberChanged,
                    onCountrySelected = viewModel::selectCountry,
                    onPickAvatar = viewModel::uploadAvatar,
                    onCompleteProfile = { viewModel.saveProfile(onComplete = onAuthenticated) }
                )
            }
        }
    }
}

@Composable
private fun AuthHeroCanvasBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = AppThemeTokens.current
    val gradientColors = listOf(
        palette.headerColor,
        palette.voiceCallGradientMid,
        palette.buttonColor
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors = gradientColors))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = minOf(size.height, 280.dp.toPx())

            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = w * 0.42f,
                center = Offset(w * 0.92f, h * 0.14f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = w * 0.34f,
                center = Offset(w * 0.06f, h * 0.78f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = w * 0.24f,
                center = Offset(w * 0.80f, h * 0.74f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            val arcPath = Path().apply {
                moveTo(0f, h * 0.38f)
                quadraticTo(w * 0.5f, h * 0.10f, w, h * 0.44f)
            }
            drawPath(
                path = arcPath,
                color = Color.White.copy(alpha = 0.07f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        content()
    }
}

@Composable
private fun WelcomeScreen(
    state: AuthUiState,
    onGoogleSignIn: (Activity) -> Unit
) {
    val activity = LocalContext.current as? Activity
    var showTermsDialog by remember { mutableStateOf(false) }
    val palette = AppThemeTokens.current
    val isDark = palette.isDark

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Atmospheric Hero Header + Elevated Curved Sheet
            AuthHeroCanvasBackground {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1A252D) else Color.White,
                            shadowElevation = 10.dp,
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                BrandLogoMark(
                                    size = 58.dp,
                                    cutoutColor = if (isDark) Color(0xFF1A252D) else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.brand_title_upper),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.tagline),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroFeatureBadge("Instant Chat")
                            HeroFeatureBadge("Voice Notes")
                            HeroFeatureBadge("HD Calls")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Elevated Curved Sheet Transition
                    Surface(
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 460.dp)
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .padding(horizontal = 24.dp, vertical = 28.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Sign in to continue",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Use your Google account for fast, secure access to your chats, voice notes, and calls.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(22.dp))

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        LoginHighlightRow(
                                            icon = Icons.Default.Bolt,
                                            tint = palette.accentColor,
                                            title = "One-tap Google sign-in",
                                            subtitle = "Get started immediately without waiting for SMS codes."
                                        )
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                        LoginHighlightRow(
                                            icon = Icons.Default.Shield,
                                            tint = palette.accentColor,
                                            title = "Private cloud sync",
                                            subtitle = "Messages, voice messages, and profile stay backed up."
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        activity?.let(onGoogleSignIn)
                                    },
                                    enabled = !state.isLoading && activity != null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("google_signin_button"),
                                    shape = RoundedCornerShape(18.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 2.dp,
                                        pressedElevation = 0.dp
                                    ),
                                    border = BorderStroke(
                                        width = 1.2.dp,
                                        color = palette.accentColor.copy(alpha = 0.45f)
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        } else {
                                            Color.White
                                        },
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    if (state.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = palette.accentColor,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Signing in...",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    } else {
                                        GoogleGLogo(size = 22.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.sign_in_with_google),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = palette.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (state.errorMessage != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    ErrorBannerCard(message = state.errorMessage)
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Secured by Firebase · ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Terms & Privacy",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = palette.accentColor,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showTermsDialog = true }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms & Privacy Policy") },
            text = {
                Text(
                    "Manoj & Sonali securely authenticates your account using Google Sign-In via Firebase Authentication and syncs your chats, voice notes, calls, statuses, and profile privately under per-user security rules."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun LoginHighlightRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = tint.copy(alpha = 0.14f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeroFeatureBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryCodePickerSheet(
    onDismiss: () -> Unit,
    onSelect: (CountryCode) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) CountryCodes.all
        else CountryCodes.all.filter {
            it.name.contains(query, ignoreCase = true) || it.dialCode.contains(query)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Select Country Code",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search country or code...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.height(360.dp)) {
                items(filtered, key = { "${it.isoCode}_${it.dialCode}" }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.flagEmoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = item.dialCode,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
private fun CreateProfileScreen(
    state: AuthUiState,
    onDisplayNameChanged: (String) -> Unit,
    onAboutChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onCountrySelected: (CountryCode) -> Unit,
    onPickAvatar: (Uri) -> Unit,
    onCompleteProfile: () -> Unit
) {
    var showCountryPicker by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickAvatar(uri)
    }
    val palette = AppThemeTokens.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthHeroCanvasBackground {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 24.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Complete your profile",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Choose how Manoj & Sonali contacts see you.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(118.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(
                                        width = 3.dp,
                                        color = palette.accentColor,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("pick_profile_avatar"),
                                contentAlignment = Alignment.Center
                            ) {
                                val previewModel: Any? = state.avatarLocalUri ?: state.uploadedAvatarUrl
                                if (previewModel != null) {
                                    AsyncImage(
                                        model = previewModel,
                                        contentDescription = "Profile photo preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Upload profile photo",
                                        tint = palette.accentColor,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                if (state.avatarUploadProgress != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { state.avatarUploadProgress },
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(palette.buttonColor)
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to upload profile photo",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = palette.accentColor
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = state.displayName,
                            onValueChange = onDisplayNameChanged,
                            label = { Text("Display Name *") },
                            placeholder = { Text("e.g. Manoj Roy") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showCountryPicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${state.selectedCountry.flagEmoji} ${state.selectedCountry.dialCode}")
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedTextField(
                                value = state.phoneNumberInput,
                                onValueChange = onPhoneChanged,
                                label = { Text("Phone Number (optional)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = state.about,
                            onValueChange = onAboutChanged,
                            label = { Text("About") },
                            maxLines = 2,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_about_input")
                        )

                        if (state.errorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            ErrorBannerCard(message = state.errorMessage)
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = onCompleteProfile,
                            enabled = !state.isLoading && state.displayName.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("save_profile_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.buttonColor)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Continue to Manoj & Sonali", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
            }
        }
    }

    if (showCountryPicker) {
        CountryCodePickerSheet(
            onDismiss = { showCountryPicker = false },
            onSelect = {
                onCountrySelected(it)
                showCountryPicker = false
            }
        )
    }
}
