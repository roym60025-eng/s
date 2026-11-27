package com.example.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.DeliveryStatus
import com.example.ui.theme.AppThemeTokens
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.ReadBlueTick

/**
 * Signature Manoj & Sonali dual overlapping speech-bubble logo:
 * warm coral-rose back bubble + vibrant mint-emerald front bubble with white signal waves and heart spark.
 */
@Composable
fun BrandLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    cutoutColor: Color = MaterialTheme.colorScheme.background
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Back bubble (warm coral-rose, bottom-left tail)
        val backBubble = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    left = w * 0.06f,
                    top = h * 0.14f,
                    right = w * 0.68f,
                    bottom = h * 0.78f
                )
            )
            moveTo(w * 0.21f, h * 0.67f)
            lineTo(w * 0.10f, h * 0.87f)
            quadraticTo(w * 0.24f, h * 0.84f, w * 0.34f, h * 0.76f)
            close()
        }
        drawPath(
            path = backBubble,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFF8F85), Color(0xFFFF5A5F)),
                start = Offset(w * 0.10f, h * 0.15f),
                end = Offset(w * 0.65f, h * 0.85f)
            )
        )

        // Subtle separation ring around front bubble
        val frontRim = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    left = w * 0.24f,
                    top = h * 0.12f,
                    right = w * 0.94f,
                    bottom = h * 0.82f
                )
            )
        }
        drawPath(
            path = frontRim,
            color = cutoutColor.copy(alpha = 0.38f),
            style = Stroke(width = w * 0.045f)
        )

        // Front bubble (vibrant mint-emerald, bottom-right tail)
        val frontBubble = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    left = w * 0.25f,
                    top = h * 0.13f,
                    right = w * 0.93f,
                    bottom = h * 0.81f
                )
            )
            moveTo(w * 0.66f, h * 0.76f)
            quadraticTo(w * 0.77f, h * 0.86f, w * 0.89f, h * 0.88f)
            lineTo(w * 0.80f, h * 0.68f)
            close()
        }
        drawPath(
            path = frontBubble,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF4ADEA8), Color(0xFF10B981)),
                start = Offset(w * 0.30f, h * 0.14f),
                end = Offset(w * 0.90f, h * 0.86f)
            )
        )

        // Crisp white voice/chat signal arcs inside front bubble
        val strokeW = (w * 0.045f).coerceAtLeast(1.5f)
        drawArc(
            color = Color.White,
            startAngle = 142f,
            sweepAngle = 76f,
            useCenter = false,
            topLeft = Offset(w * 0.38f, h * 0.28f),
            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.38f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White,
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(w * 0.45f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.24f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Clean centered heart spark mark inside the front bubble
        val hx = w * 0.68f
        val hy = h * 0.48f
        val hs = w * 0.095f
        val heart = Path().apply {
            moveTo(hx, hy + hs * 0.85f)
            cubicTo(hx - hs * 1.35f, hy + hs * 0.05f, hx - hs * 1.15f, hy - hs * 1.05f, hx, hy - hs * 0.28f)
            cubicTo(hx + hs * 1.15f, hy - hs * 1.05f, hx + hs * 1.35f, hy + hs * 0.05f, hx, hy + hs * 0.85f)
            close()
        }
        drawPath(path = heart, color = Color.White)
    }
}

/**
 * Flowing emerald-green ribbons across top-left and bottom-right corners matching the showcase splash/login screens.
 */
@Composable
fun WavyBrandBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = AppThemeTokens.current
    val isDark = palette.isDark
    val baseColor = MaterialTheme.colorScheme.background
    val topAlpha = if (isDark) 0.24f else 0.88f
    val softAlpha = if (isDark) 0.14f else 0.32f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft secondary top wave
            val topSoftWave = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.08f)
                cubicTo(w * 0.70f, h * 0.20f, w * 0.35f, h * 0.04f, 0f, h * 0.22f)
                close()
            }
            drawPath(
                path = topSoftWave,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.waveSoftColor.copy(alpha = softAlpha),
                        palette.accentColor.copy(alpha = softAlpha * 0.6f)
                    )
                )
            )

            // Primary vibrant top-left wave
            val topMainWave = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.82f, 0f)
                cubicTo(w * 0.54f, h * 0.14f, w * 0.28f, h * 0.05f, 0f, h * 0.17f)
                close()
            }
            drawPath(
                path = topMainWave,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.accentColor.copy(alpha = topAlpha),
                        palette.waveSoftColor.copy(alpha = topAlpha * 0.85f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.7f, h * 0.15f)
                )
            )

            // Soft secondary bottom wave
            val bottomSoftWave = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.84f)
                cubicTo(w * 0.34f, h * 0.94f, w * 0.64f, h * 0.76f, w, h * 0.85f)
                lineTo(w, h)
                close()
            }
            drawPath(
                path = bottomSoftWave,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.waveSoftColor.copy(alpha = softAlpha * 0.7f),
                        palette.accentColor.copy(alpha = softAlpha)
                    )
                )
            )

            // Primary bottom wave
            val bottomMainWave = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.90f)
                cubicTo(w * 0.36f, h * 0.97f, w * 0.66f, h * 0.83f, w, h * 0.90f)
                lineTo(w, h)
                close()
            }
            drawPath(
                path = bottomMainWave,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.accentColor.copy(alpha = topAlpha * 0.92f),
                        palette.waveDeepColor.copy(alpha = topAlpha)
                    ),
                    start = Offset(0f, h * 0.85f),
                    end = Offset(w, h)
                )
            )
        }
        content()
    }
}

/**
 * Showcase Splash Screen (#1): Branded heart speech-bubble logo, "Manoj & Sonali", "Messaging App",
 * and "Stay Connected Always..." with subtle curved green accent stroke.
 */
@Composable
fun BrandedSplashScreen(modifier: Modifier = Modifier) {
    val palette = AppThemeTokens.current
    WavyBrandBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandLogoMark(size = 118.dp)
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.brand_title_upper),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 33.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = palette.accentColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.messaging_app_subtitle),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 21.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(34.dp))
            Text(
                text = "Stay Connected\nAlways…",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Canvas(modifier = Modifier.width(132.dp).height(14.dp)) {
                val path = Path().apply {
                    moveTo(4f, size.height * 0.78f)
                    quadraticTo(size.width * 0.45f, 0f, size.width - 4f, size.height * 0.45f)
                }
                drawPath(
                    path = path,
                    color = palette.accentColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Subtle warm wallpaper with delicate low-contrast geometric doodle motifs for ChatDetailScreen & GroupChatScreen.
 */
@Composable
fun ChatDoodleWallpaper(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = AppThemeTokens.current
    val isDark = palette.isDark
    val bgColor = palette.chatWallpaperColor
    val motifColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFF7B7060).copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 64.dp.toPx()
            val cols = (size.width / step).toInt() + 2
            val rows = (size.height / step).toInt() + 2
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val cx = c * step + if (r % 2 == 0) 0f else step * 0.5f
                    val cy = r * step
                    when ((r + c) % 3) {
                        0 -> drawCircle(
                            color = motifColor,
                            radius = 7.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        1 -> drawLine(
                            color = motifColor,
                            start = Offset(cx - 6.dp.toPx(), cy + 4.dp.toPx()),
                            end = Offset(cx + 6.dp.toPx(), cy - 4.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        else -> drawCircle(
                            color = motifColor,
                            radius = 2.5.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
        }
        content()
    }
}

/**
 * Compact multicolor 'G' Google emblem drawn on Canvas for the "Continue with Google" button.
 */
@Composable
fun GoogleGLogo(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.width * 0.22f
        val r = (this.size.width - strokeWidth) / 2f
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = androidx.compose.ui.geometry.Size(r * 2f, r * 2f)

        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -145f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 35f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -5f,
            sweepAngle = 50f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(this.size.width * 0.50f, this.size.height * 0.50f),
            end = Offset(this.size.width * 0.94f, this.size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt
        )
    }
}

@Composable
fun UserAvatar(
    name: String,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    hasStatusRing: Boolean = false,
    statusViewed: Boolean = false
) {
    val palette = AppThemeTokens.current
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"
    val ringColor = if (statusViewed) {
        MaterialTheme.colorScheme.outline
    } else {
        palette.accentColor
    }
    val ringModifier = if (hasStatusRing) {
        Modifier
            .border(
                width = 2.5.dp,
                color = ringColor,
                shape = CircleShape
            )
            .padding(3.dp)
    } else {
        Modifier
    }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(ringModifier)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "$name avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.28f).dp.coerceIn(10.dp, 15.dp))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(OnlineEmerald)
            )
        }
    }
}

@Composable
fun DeliveryTickIcon(
    status: DeliveryStatus,
    tintDefault: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    when (status) {
        DeliveryStatus.SENT -> Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Sent",
            tint = tintDefault,
            modifier = Modifier.size(15.dp)
        )
        DeliveryStatus.DELIVERED -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "Delivered",
            tint = tintDefault,
            modifier = Modifier.size(15.dp)
        )
        DeliveryStatus.READ -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "Read",
            tint = ReadBlueTick,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
fun PulsingLiveDot(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(OnlineEmerald.copy(alpha = alpha))
    )
}

@Composable
fun EmptyIllustrationState(
    drawableRes: Int? = null,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(164.dp)
                    .clip(RoundedCornerShape(28.dp))
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = AppThemeTokens.current.buttonColor)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun LoadingStateBox(
    label: String = "Syncing conversations...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(strokeWidth = 3.dp, color = AppThemeTokens.current.accentColor)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorBannerCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
