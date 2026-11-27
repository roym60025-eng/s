package com.example.photos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.GoogleGLogo
import com.example.presentation.components.UserAvatar
import com.example.ui.theme.AppThemeTokens

@Composable
fun GooglePhotosTabScreen(
    viewModel: GooglePhotosViewModel,
    searchQuery: String
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewItem by remember { mutableStateOf<GooglePhotoItem?>(null) }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (activity != null) {
            viewModel.onConsentIntentResult(
                activity = activity,
                resultOk = result.resultCode == Activity.RESULT_OK,
                data = result.data
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GooglePhotosEvent.LaunchConsent -> {
                    consentLauncher.launch(
                        IntentSenderRequest.Builder(event.pendingIntent.intentSender).build()
                    )
                }
                is GooglePhotosEvent.OpenPickerUri -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.uri)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshActiveSessionIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!state.isSignedIn) {
        GooglePhotosSignInView(
            state = state,
            onSignIn = { activity?.let(viewModel::signInToGooglePhotos) }
        )
    } else {
        GooglePhotosLibraryView(
            state = state,
            searchQuery = searchQuery,
            onPickPhotos = { activity?.let(viewModel::openGooglePhotosPicker) },
            onRefresh = viewModel::refreshManually,
            onReconnect = { activity?.let(viewModel::signInToGooglePhotos) },
            onSignOut = viewModel::signOutGooglePhotos,
            onOpenPreview = { previewItem = it }
        )
    }

    previewItem?.let { item ->
        GooglePhotoPreviewDialog(
            item = item,
            accessToken = state.accessToken,
            onDismiss = { previewItem = null }
        )
    }
}

@Composable
private fun GooglePhotosSignInView(
    state: GooglePhotosUiState,
    onSignIn: () -> Unit
) {
    val palette = AppThemeTokens.current
    val isDark = palette.isDark

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Banner Card
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        palette.headerColor,
                                        palette.voiceCallGradientMid,
                                        palette.buttonColor
                                    )
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF1A252D) else Color.White,
                                shadowElevation = 8.dp,
                                modifier = Modifier.size(84.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    GooglePhotosPinwheelLogo(size = 48.dp)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Google Photos",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sign in with your Google Account to connect and view your Google Photos securely.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.88f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PhotosTrustRow(
                            icon = Icons.Default.Shield,
                            tint = palette.accentColor,
                            title = "Official Google OAuth 2.0",
                            subtitle = "Grant scoped read-only access directly through Google Play Services."
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        PhotosTrustRow(
                            icon = Icons.Default.CloudDone,
                            tint = palette.accentColor,
                            title = "Private Cloud Picker",
                            subtitle = "Pick specific albums, photos, or videos without exposing your entire library."
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = onSignIn,
                            enabled = !state.isAuthorizing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_photos_signin_button"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = palette.accentColor.copy(alpha = 0.45f)
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (state.isAuthorizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = palette.accentColor,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Connecting Google Photos...",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            } else {
                                GoogleGLogo(size = 22.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sign in to Google Photos",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }

                        if (state.errorMessage != null) {
                            ErrorBannerCard(message = state.errorMessage)
                        }

                        if (state.infoMessage != null && state.errorMessage == null) {
                            Text(
                                text = state.infoMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.accentColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

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
                                text = "Read-only Google Photos Picker access · Sign out anytime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun GooglePhotosLibraryView(
    state: GooglePhotosUiState,
    searchQuery: String,
    onPickPhotos: () -> Unit,
    onRefresh: () -> Unit,
    onReconnect: () -> Unit,
    onSignOut: () -> Unit,
    onOpenPreview: (GooglePhotoItem) -> Unit
) {
    val palette = AppThemeTokens.current
    val context = LocalContext.current
    val q = searchQuery.trim().lowercase()
    val filteredItems = remember(state.items, q) {
        if (q.isEmpty()) state.items
        else state.items.filter {
            it.filename.lowercase().contains(q) ||
                it.type.lowercase().contains(q) ||
                (it.createTime?.lowercase()?.contains(q) == true)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 116.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "gphotos_account_card") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        UserAvatar(
                            name = state.accountName ?: state.accountEmail ?: "G",
                            photoUrl = state.accountPhotoUrl,
                            size = 50.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.accountName ?: "Google Photos Connected",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Connected",
                                    tint = palette.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (!state.accountEmail.isNullOrBlank()) {
                                Text(
                                    text = state.accountEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("google_photos_signout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out Google Photos",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPickPhotos,
                            enabled = !state.isLoadingPhotos && !state.isAuthorizing,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_google_photos_picker_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.buttonColor)
                        ) {
                            if (state.isLoadingPhotos) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select from Google Photos")
                            }
                        }

                        if (state.activeSessionId != null) {
                            OutlinedButton(
                                onClick = onRefresh,
                                enabled = !state.isLoadingPhotos,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Google Photos",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (state.accessToken == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onReconnect,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Refresh Google Authorization", color = palette.accentColor)
                        }
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "gphotos_error") {
                ErrorBannerCard(message = state.errorMessage)
            }
        } else if (state.infoMessage != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "gphotos_info") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.infoMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = palette.buttonColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "gphotos_empty") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GooglePhotosPinwheelLogo(size = 56.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (q.isNotEmpty()) {
                                "No Google Photos match \"$searchQuery\""
                            } else {
                                "Your Google Photos Account is Ready"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (q.isNotEmpty()) {
                                "Clear the search query to see your imported Google Photos."
                            } else {
                                "Tap \"Select from Google Photos\" above to choose photos or videos from your Google Photos library."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { photo ->
                val thumbUrl = "${photo.baseUrl}=w420-h420-c"
                val imageRequest = remember(thumbUrl, state.accessToken) {
                    ImageRequest.Builder(context)
                        .data(thumbUrl)
                        .apply {
                            if (!state.accessToken.isNullOrBlank()) {
                                addHeader("Authorization", "Bearer ${state.accessToken}")
                            }
                        }
                        .crossfade(true)
                        .build()
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenPreview(photo) }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = photo.filename,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (photo.type.equals("VIDEO", ignoreCase = true)) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(34.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GooglePhotoPreviewDialog(
    item: GooglePhotoItem,
    accessToken: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fullUrl = "${item.baseUrl}=w1600-h1600"
    val request = remember(fullUrl, accessToken) {
        ImageRequest.Builder(context)
            .data(fullUrl)
            .apply {
                if (!accessToken.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $accessToken")
                }
            }
            .build()
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
        ) {
            AsyncImage(
                model = request,
                contentDescription = item.filename,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        text = item.filename,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.width > 0 && item.height > 0) {
                        Text(
                            text = "${item.width} × ${item.height}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotosTrustRow(
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

/**
 * Clean 4-petal Google Photos inspired pinwheel emblem rendered on Canvas.
 */
@Composable
fun GooglePhotosPinwheelLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = w * 0.22f

        // Top-left red petal (semicircle pointing up)
        val redPetal = Path().apply {
            moveTo(cx - r * 2f, cy)
            cubicTo(cx - r * 2f, cy - r * 1.4f, cx, cy - r * 1.4f, cx, cy)
            close()
        }
        drawPath(redPetal, Color(0xFFEA4335))

        // Top-right yellow petal (semicircle pointing right)
        val yellowPetal = Path().apply {
            moveTo(cx, cy - r * 2f)
            cubicTo(cx + r * 1.4f, cy - r * 2f, cx + r * 1.4f, cy, cx, cy)
            close()
        }
        drawPath(yellowPetal, Color(0xFFFBBC04))

        // Bottom-right green petal (semicircle pointing down)
        val greenPetal = Path().apply {
            moveTo(cx + r * 2f, cy)
            cubicTo(cx + r * 2f, cy + r * 1.4f, cx, cy + r * 1.4f, cx, cy)
            close()
        }
        drawPath(greenPetal, Color(0xFF34A853))

        // Bottom-left blue petal (semicircle pointing left)
        val bluePetal = Path().apply {
            moveTo(cx, cy + r * 2f)
            cubicTo(cx - r * 1.4f, cy + r * 2f, cx - r * 1.4f, cy, cx, cy)
            close()
        }
        drawPath(bluePetal, Color(0xFF4285F4))

        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = w * 0.055f,
            center = Offset(cx, cy)
        )
    }
}
