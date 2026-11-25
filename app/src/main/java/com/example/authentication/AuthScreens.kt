package com.example.authentication

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.presentation.components.WavyBrandBackdrop
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
            AuthStep.WELCOME -> WelcomeScreen(
                state = state,
                onCountrySelected = viewModel::selectCountry,
                onPhoneChanged = viewModel::onPhoneNumberChanged,
                onSendOtp = { activity ->
                    viewModel.sendOtp(activity, onAutoVerifiedReady = onAuthenticated)
                },
                onGoogleSignIn = { activity ->
                    viewModel.signInWithGoogle(activity, onSignedInAndReady = onAuthenticated)
                }
            )

            AuthStep.PHONE_INPUT -> {
                BackHandler { viewModel.navigateTo(AuthStep.WELCOME) }
                PhoneLoginScreen(
                    state = state,
                    onBack = { viewModel.navigateTo(AuthStep.WELCOME) },
                    onCountrySelected = viewModel::selectCountry,
                    onPhoneChanged = viewModel::onPhoneNumberChanged,
                    onSendOtp = { activity ->
                        viewModel.sendOtp(activity, onAutoVerifiedReady = onAuthenticated)
                    },
                    onGoogleSignIn = { activity ->
                        viewModel.signInWithGoogle(activity, onSignedInAndReady = onAuthenticated)
                    }
                )
            }

            AuthStep.OTP_VERIFY -> {
                BackHandler { viewModel.navigateTo(AuthStep.WELCOME) }
                OtpVerificationScreen(
                    state = state,
                    onBack = { viewModel.navigateTo(AuthStep.WELCOME) },
                    onOtpChanged = viewModel::onOtpChanged,
                    onVerify = { viewModel.verifyOtp(onProfileExists = onAuthenticated) },
                    onResend = { activity ->
                        viewModel.sendOtp(activity, isResend = true, onAutoVerifiedReady = onAuthenticated)
                    }
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
private fun BrandLockup(
    logoSizeDp: Int = 86,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandLogoMark(size = logoSizeDp.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.brand_title_upper),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            ),
            color = AppThemeTokens.current.accentColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.messaging_app_subtitle),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WelcomeScreen(
    state: AuthUiState,
    onCountrySelected: (CountryCode) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onSendOtp: (Activity) -> Unit,
    onGoogleSignIn: (Activity) -> Unit
) {
    val activity = LocalContext.current as? Activity
    var showCountryPicker by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        WavyBrandBackdrop {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 26.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(18.dp))
                    BrandLockup(logoSizeDp = 90)
                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "Login to continue",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Unified pill mobile number field matching showcase Login Screen
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCountryPicker = true }
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                    .testTag("country_code_selector"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.selectedCountry.dialCode,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select country code",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .width(1.dp)
                                    .height(22.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                            )
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (state.phoneNumberInput.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.enter_mobile_number),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                    )
                                }
                                BasicTextField(
                                    value = state.phoneNumberInput,
                                    onValueChange = onPhoneChanged,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(AppThemeTokens.current.accentColor),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (!state.isLoading && state.phoneNumberInput.length >= 6) {
                                                activity?.let(onSendOtp)
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("phone_number_input")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val buttonColor = AppThemeTokens.current.buttonColor
                    Button(
                        onClick = { activity?.let(onSendOtp) },
                        enabled = !state.isLoading && state.phoneNumberInput.length >= 6 && activity != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("send_otp_button"),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = Color.White,
                            disabledContainerColor = buttonColor.copy(alpha = 0.45f),
                            disabledContentColor = Color.White.copy(alpha = 0.85f)
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.send_otp),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        ErrorBannerCard(message = state.errorMessage)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "  OR  ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedButton(
                        onClick = { activity?.let(onGoogleSignIn) },
                        enabled = !state.isLoading && activity != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_signin_button"),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        GoogleGLogo(size = 20.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.sign_in_with_google),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    Text(
                        text = "By continuing, you agree to our",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Terms & Privacy Policy",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AppThemeTokens.current.accentColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTermsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms & Privacy Policy") },
            text = {
                Text(
                    "Manoj & Sonali securely authenticates your account using Firebase Authentication (Google Sign-In or Phone OTP) and syncs your chats, calls, statuses, and profile privately under per-user security rules."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Close") }
            }
        )
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
    WelcomeScreen(
        state = state,
        onCountrySelected = onCountrySelected,
        onPhoneChanged = onPhoneChanged,
        onSendOtp = onSendOtp,
        onGoogleSignIn = onGoogleSignIn
    )
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

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                BrandLockup(logoSizeDp = 82)
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.verify_otp_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.infoMessage ?: "Enter the 6 digit code sent to",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.selectedCountry.dialCode} ${state.phoneNumberInput}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                // 6-box segmented OTP input matching showcase #3
                OtpSixDigitBoxes(
                    code = state.otpCode,
                    onCodeChange = onOtpChanged,
                    onDone = {
                        if (state.otpCode.length == 6 && !state.isLoading) onVerify()
                    }
                )

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ErrorBannerCard(message = state.errorMessage)
                }

                Spacer(modifier = Modifier.height(28.dp))

                val palette = AppThemeTokens.current
                Button(
                    onClick = onVerify,
                    enabled = state.otpCode.length == 6 && !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("verify_otp_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.buttonColor,
                        contentColor = Color.White,
                        disabledContainerColor = palette.buttonColor.copy(alpha = 0.45f),
                        disabledContentColor = Color.White.copy(alpha = 0.85f)
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.verify_button),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (state.resendCountdownSec > 0) {
                    val mm = (state.resendCountdownSec / 60).toString().padStart(2, '0')
                    val ss = (state.resendCountdownSec % 60).toString().padStart(2, '0')
                    Text(
                        text = "Resend OTP ($mm:$ss)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    TextButton(
                        onClick = { activity?.let(onResend) },
                        enabled = !state.isLoading && activity != null
                    ) {
                        Text(
                            text = "Resend OTP",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpSixDigitBoxes(
    code: String,
    onCodeChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            repeat(6) { index ->
                val char = code.getOrNull(index)?.toString() ?: ""
                val isFocusedSlot = index == code.length.coerceAtMost(5)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isFocusedSlot || char.isNotEmpty()) 1.5.dp else 1.dp,
                        color = if (isFocusedSlot) AppThemeTokens.current.accentColor else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.size(width = 46.dp, height = 54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        BasicTextField(
            value = code,
            onValueChange = { onCodeChange(it.filter { ch -> ch.isDigit() }.take(6)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .alpha(0.01f)
                .testTag("otp_code_input")
        )
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create your profile",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = AppThemeTokens.current.headerColor
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
                        color = AppThemeTokens.current.accentColor,
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
                        tint = AppThemeTokens.current.accentColor,
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
                color = AppThemeTokens.current.accentColor
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
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppThemeTokens.current.buttonColor)
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
