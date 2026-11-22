package com.example.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.authentication.CountryCodePickerSheet
import com.example.domain.model.SavedContact
import com.example.domain.model.UserProfile
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.UserAvatar
import com.example.security.InputValidator
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.SoftBlueTertiary
import com.example.ui.theme.VioletSecondary
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    initialTab: AddContactTab,
    viewModel: ContactsViewModel,
    onBack: () -> Unit,
    onOpenMyQr: () -> Unit,
    onOpenChat: (String) -> Unit,
    onStartVoiceCall: (UserProfile) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedContacts by viewModel.savedContacts.collectAsStateWithLifecycle()

    LaunchedEffect(initialTab) {
        viewModel.setInitialTab(initialTab)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_contact_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Add Contact") },
                actions = {
                    IconButton(
                        onClick = onOpenMyQr,
                        modifier = Modifier.testTag("open_my_qr_button")
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = "My QR Code")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = state.activeTab == AddContactTab.PHONE,
                    onClick = { viewModel.selectTab(AddContactTab.PHONE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_add_by_phone")
                ) {
                    Text("Phone Number")
                }
                SegmentedButton(
                    selected = state.activeTab == AddContactTab.QR_SCAN,
                    onClick = { viewModel.selectTab(AddContactTab.QR_SCAN) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_scan_qr")
                ) {
                    Text("Scan QR")
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                when (state.activeTab) {
                    AddContactTab.PHONE -> AddByPhoneTabContent(
                        state = state,
                        savedContacts = savedContacts,
                        onCountrySelected = viewModel::selectCountry,
                        onPhoneChanged = viewModel::onPhoneInputChanged,
                        onSearch = viewModel::lookupByPhone,
                        onSaveOnly = { viewModel.saveFoundContact(openChatAfterSave = false, onChatReady = onOpenChat) },
                        onSaveAndChat = { viewModel.saveFoundContact(openChatAfterSave = true, onChatReady = onOpenChat) },
                        onDismissFound = viewModel::dismissFoundContact,
                        onVoiceCall = onStartVoiceCall,
                        onOpenSavedChat = { contact ->
                            viewModel.openChatWithSavedContact(contact.contactUid, onOpenChat)
                        },
                        onDeleteSaved = { contact ->
                            viewModel.removeSavedContact(contact.contactUid)
                        }
                    )

                    AddContactTab.QR_SCAN -> QrScannerTabContent(
                        state = state,
                        onQrDetected = viewModel::onQrDetected,
                        onScanGallery = viewModel::scanFromGalleryImage,
                        onSaveOnly = { viewModel.saveFoundContact(openChatAfterSave = false, onChatReady = onOpenChat) },
                        onSaveAndChat = { viewModel.saveFoundContact(openChatAfterSave = true, onChatReady = onOpenChat) },
                        onDismissFound = viewModel::dismissFoundContact,
                        onOpenMyQr = onOpenMyQr
                    )
                }
            }
        }
    }
}

@Composable
private fun AddByPhoneTabContent(
    state: AddContactUiState,
    savedContacts: List<SavedContact>,
    onCountrySelected: (com.example.utils.CountryCode) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onSaveOnly: () -> Unit,
    onSaveAndChat: () -> Unit,
    onDismissFound: () -> Unit,
    onVoiceCall: (UserProfile) -> Unit,
    onOpenSavedChat: (SavedContact) -> Unit,
    onDeleteSaved: (SavedContact) -> Unit
) {
    var showCountryPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add contact by phone number",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter their country code and phone number registered on Manoj & Sonali.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showCountryPicker = true }
                                .testTag("add_contact_country_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.selectedCountry.flagEmoji} ${state.selectedCountry.dialCode}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Country code")
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedTextField(
                            value = state.phoneInput,
                            onValueChange = onPhoneChanged,
                            label = { Text("Phone number") },
                            placeholder = { Text("9876543210") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_contact_phone_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSearch,
                        enabled = !state.isSearching && state.phoneInput.filter { it.isDigit() }.length >= 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("find_contact_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (state.isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Find Contact")
                        }
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            item { ErrorBannerCard(message = state.errorMessage) }
        }

        if (state.infoMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = OnlineEmerald.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.infoMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnlineEmerald,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        state.foundUser?.let { user ->
            item {
                FoundContactCard(
                    user = user,
                    phoneDisplay = state.foundPhoneDisplay,
                    isSaving = state.isSaving,
                    onSaveOnly = onSaveOnly,
                    onSaveAndChat = onSaveAndChat,
                    onVoiceCall = { onVoiceCall(user) },
                    onDismiss = onDismissFound
                )
            }
        }

        if (savedContacts.isNotEmpty()) {
            item {
                Text(
                    text = "Saved Contacts (${savedContacts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            items(savedContacts, key = { it.contactUid }) { contact ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSavedChat(contact) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = contact.displayName,
                            photoUrl = contact.photoUrl,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = contact.phoneNumber ?: contact.about.ifBlank { "Added via ${contact.addedVia}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onOpenSavedChat(contact) }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteSaved(contact) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove contact")
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

@Composable
private fun FoundContactCard(
    user: UserProfile,
    phoneDisplay: String?,
    isSaving: Boolean,
    onSaveOnly: () -> Unit,
    onSaveAndChat: () -> Unit,
    onVoiceCall: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("found_contact_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    name = user.displayName,
                    photoUrl = user.photoUrl,
                    size = 56.dp,
                    isOnline = user.online
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                    if (!phoneDisplay.isNullOrBlank()) {
                        Text(
                            text = phoneDisplay,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = user.about,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveOnly,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f).testTag("save_contact_only_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
                Button(
                    onClick = onSaveAndChat,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f).testTag("save_and_chat_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Message")
                }
                if (onVoiceCall != null) {
                    FilledTonalButton(
                        onClick = onVoiceCall,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Voice call", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun QrScannerTabContent(
    state: AddContactUiState,
    onQrDetected: (String) -> Unit,
    onScanGallery: (android.content.Context, android.net.Uri) -> Unit,
    onSaveOnly: () -> Unit,
    onSaveAndChat: () -> Unit,
    onDismissFound: () -> Unit,
    onOpenMyQr: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onScanGallery(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (hasCameraPermission) {
            val previewView = remember { PreviewView(context) }
            val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
            val barcodeScanner = remember {
                BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                )
            }

            DisposableEffect(lifecycleOwner) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val mainExecutor = ContextCompat.getMainExecutor(context)

                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                                            if (!raw.isNullOrBlank()) {
                                                mainExecutor.execute { onQrDetected(raw) }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    try {
                        provider.unbindAll()
                        boundCamera = provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (_: Exception) {
                    }
                }, mainExecutor)

                onDispose {
                    try {
                        cameraProviderFuture.get().unbindAll()
                    } catch (_: Exception) {
                    }
                    barcodeScanner.close()
                    analysisExecutor.shutdown()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Viewfinder corner reticle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameSize = minOf(size.width, size.height) * 0.62f
                    val left = (size.width - frameSize) / 2f
                    val top = (size.height - frameSize) / 2.2f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(left, top),
                        size = Size(frameSize, frameSize),
                        cornerRadius = CornerRadius(24f, 24f),
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                }

                // Top scanner controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "Point at a Manoj & Sonali QR",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val next = !torchEnabled
                            torchEnabled = next
                            boundCamera?.cameraControl?.enableTorch(next)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Toggle flashlight",
                            tint = Color.White
                        )
                    }
                }

                // Bottom actions inside camera container
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.isSearching) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Looking up scanned contact...")
                            }
                        }
                    }

                    if (state.errorMessage != null) {
                        ErrorBannerCard(message = state.errorMessage)
                    }

                    if (state.infoMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.infoMessage,
                                color = OnlineEmerald,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    state.foundUser?.let { found ->
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FoundContactCard(
                                user = found,
                                phoneDisplay = state.foundPhoneDisplay,
                                isSaving = state.isSaving,
                                onSaveOnly = onSaveOnly,
                                onSaveAndChat = onSaveAndChat,
                                onDismiss = onDismissFound
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("scan_qr_gallery_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("From Gallery")
                        }
                        FilledTonalButton(
                            onClick = onOpenMyQr,
                            modifier = Modifier.weight(1f).testTag("scanner_my_qr_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("My QR Code")
                        }
                    }
                }
            }
        } else {
            // Permission denied / not granted yet -> offer camera grant + Gallery QR fallback
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Scan QR Code",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allow camera access to scan a contact's QR code live, or pick a saved QR screenshot from your gallery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Allow Camera Scanner")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan QR from Gallery")
                    }
                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        ErrorBannerCard(message = state.errorMessage)
                    }
                    state.foundUser?.let { found ->
                        Spacer(modifier = Modifier.height(14.dp))
                        FoundContactCard(
                            user = found,
                            phoneDisplay = state.foundPhoneDisplay,
                            isSaving = state.isSaving,
                            onSaveOnly = onSaveOnly,
                            onSaveAndChat = onSaveAndChat,
                            onDismiss = onDismissFound
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQrCodeScreen(
    viewModel: ContactsViewModel,
    onBack: () -> Unit,
    onOpenScanner: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val qrPayload = remember(viewModel.currentUserId) {
        InputValidator.buildContactQrPayload(viewModel.currentUserId)
    }
    val qrBitmap = remember(qrPayload) {
        QrCodeUtils.generateQrImageBitmap(qrPayload, sizePx = 640)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("my_qr_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("My QR Code") },
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Add me on Manoj & Sonali: $qrPayload"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Contact QR"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share contact link")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary, SoftBlueTertiary)))
                            .padding(3.dp)
                    ) {
                        UserAvatar(
                            name = profile?.displayName ?: "Member",
                            photoUrl = profile?.photoUrl,
                            size = 76.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile?.displayName ?: "Manoj & Sonali Member",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Manoj & Sonali Contact QR",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        tonalElevation = 2.dp
                    ) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "Your personal contact QR code",
                            modifier = Modifier
                                .padding(16.dp)
                                .size(228.dp)
                                .testTag("my_qr_image")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your friends can scan this QR code using the Manoj & Sonali scanner to add you instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onOpenScanner,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("my_qr_scan_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Contact QR Code")
            }
        }
    }
}
