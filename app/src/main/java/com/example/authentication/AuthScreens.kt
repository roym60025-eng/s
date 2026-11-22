package com.example.authentication

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.presentation.components.BrandLogoMark
import com.example.presentation.components.ErrorBannerCard
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SoftBlueTertiary
import com.example.ui.theme.VioletSecondary
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
            AuthStep.WELCOME -> WelcomeScreen(
                state = state,
                onGoogleSignIn = { activity ->
                    viewModel.signInWithGoogle(activity, onSignedInAndReady = onAuthenticated)
                },
                onPhoneLoginClick = { viewModel.navigateTo(AuthStep.PHONE_INPUT) }
            )

            AuthStep.PHONE_INPUT -> {
                BackHandler { viewModel.navigateTo(AuthStep.WELCOME) }
                PhoneLoginScreen(
                    state = state,
                    onBack = { viewModel.navigateTo(AuthStep.WELCOME) },
                    onCountrySelected = viewModel::selectCountry,
                    onPhoneChanged = viewModel::onPhoneNumberChanged,
                    onSendOtp = { activity -> viewModel.sendOtp(activity) },
                    onGoogleSignIn = { activity ->
                        viewModel.signInWithGoogle(activity, onSignedInAndReady = onAuthenticated)
                    }
                )
            }

            AuthStep.OTP_VERIFY -> {
                BackHandler { viewModel.navigateTo(AuthStep.PHONE_INPUT) }
                OtpVerificationScreen(
                    state = state,
                    onBack = { viewModel.navigateTo(AuthStep.PHONE_INPUT) },
                    onOtpChanged = viewModel::onOtpChanged,
                    onVerify = { viewModel.verifyOtp(onProfileExists = onAuthenticated) },
                    onResend = { activity -> viewModel.sendOtp(activity, isResend = true) }
                )
            }

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
private fun WelcomeScreen(
    state: AuthUiState,
    onGoogleSignIn: (Activity) -> Unit,
    onPhoneLoginClick: () -> Unit
) {
    val activity = LocalContext.current as? Activity

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BrandLogoMark(size = 42.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.brand_title_upper),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(30.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_welcome_hero),
                            contentDescription = "Manoj & Sonali welcome artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xCC120D2B))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Protected Firebase Cloud Sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Conversations that feel personal and effortless.",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Real-time chats, rich media, 24-hour status stories, group spaces, and live voice & video calling in one refined workspace.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.errorMessage != null) {
                        ErrorBannerCard(message = state.errorMessage)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Button(
                        onClick = { activity?.let(onGoogleSignIn) },
                        enabled = !state.isLoading && activity != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("google_signin_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.sign_in_with_google),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onPhoneLoginClick,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("phone_signin_button"),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.continue_with_phone),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Instant sign-in with Google is pre-provisioned and ready immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneLoginScreen(
    state: AuthUiState,
    onBack: () -> Unit,
    onCountrySelected: (CountryCode) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onSendOtp: (Activity) -> Unit,
    onGoogleSignIn: (Activity) -> Unit
) {
    val activity = LocalContext.current as? Activity
    var showCountryPicker by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("phone_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Enter your phone number",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We will send a real 6-digit verification code via Firebase Phone Authentication.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .height(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showCountryPicker = true }
                            .testTag("country_code_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${state.selectedCountry.flagEmoji} ${state.selectedCountry.dialCode}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select country code"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedTextField(
                        value = state.phoneNumberInput,
                        onValueChange = onPhoneChanged,
                        label = { Text("Phone number") },
                        placeholder = { Text("9876543210") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("phone_number_input")
                    )
                }

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ErrorBannerCard(message = state.errorMessage)
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { activity?.let(onGoogleSignIn) },
                        enabled = !state.isLoading && activity != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("phone_fallback_google_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Google instead", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Column {
                Button(
                    onClick = { activity?.let(onSendOtp) },
                    enabled = !state.isLoading && state.phoneNumberInput.length >= 6 && activity != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("send_otp_button"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send OTP Code", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { activity?.let(onGoogleSignIn) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Or sign in instantly with Google")
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
private fun OtpVerificationScreen(
    state: AuthUiState,
    onBack: () -> Unit,
    onOtpChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: (Activity) -> Unit
) {
    val activity = LocalContext.current as? Activity

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Verify your number",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter the 6-digit code sent to ${state.selectedCountry.dialCode} ${state.phoneNumberInput}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = state.otpCode,
                    onValueChange = onOtpChanged,
                    label = { Text("6-digit OTP code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("otp_code_input")
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.resendCountdownSec > 0) {
                            "Resend available in ${state.resendCountdownSec}s"
                        } else {
                            "Didn't receive the code?"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { activity?.let(onResend) },
                        enabled = state.resendCountdownSec == 0 && !state.isLoading && activity != null
                    ) {
                        Text("Resend OTP")
                    }
                }

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ErrorBannerCard(message = state.errorMessage)
                }
            }

            Button(
                onClick = onVerify,
                enabled = state.otpCode.length == 6 && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("verify_otp_button"),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Verify & Continue", style = MaterialTheme.typography.titleMedium)
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

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create your profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose how Manoj & Sonali contacts see you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary, SoftBlueTertiary)),
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
                        tint = MaterialTheme.colorScheme.primary,
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

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to upload profile photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChanged,
                label = { Text("Display Name *") },
                placeholder = { Text("e.g. Manoj Sharma") },
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
                    .height(56.dp)
                    .testTag("save_profile_button"),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Continue to Manoj & Sonali", style = MaterialTheme.typography.titleMedium)
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
