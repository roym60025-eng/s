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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.model.DeliveryStatus
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.ReadBlueTick
import com.example.ui.theme.SoftBlueTertiary
import com.example.ui.theme.VioletSecondary

@Composable
fun BrandLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val brush = Brush.linearGradient(
            colors = listOf(IndigoPrimary, VioletSecondary, SoftBlueTertiary),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawRoundRect(
            brush = brush,
            topLeft = Offset(w * 0.06f, h * 0.08f),
            size = Size(w * 0.88f, h * 0.78f),
            cornerRadius = CornerRadius(w * 0.30f, h * 0.30f)
        )
        // Subtle chat tail
        val tail = Path().apply {
            moveTo(w * 0.24f, h * 0.82f)
            lineTo(w * 0.14f, h * 0.96f)
            lineTo(w * 0.42f, h * 0.85f)
            close()
        }
        drawPath(tail, brush = brush)

        // Subtle infinity connection wave inside bubble
        val wave = Path().apply {
            moveTo(w * 0.28f, h * 0.48f)
            cubicTo(w * 0.36f, h * 0.30f, w * 0.48f, h * 0.66f, w * 0.56f, h * 0.48f)
            cubicTo(w * 0.62f, h * 0.36f, w * 0.68f, h * 0.42f, w * 0.73f, h * 0.48f)
        }
        drawPath(
            path = wave,
            color = Color.White,
            style = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.05f,
            center = Offset(w * 0.27f, h * 0.48f)
        )
        drawCircle(
            color = Color(0xFFB4E2FF),
            radius = w * 0.055f,
            center = Offset(w * 0.73f, h * 0.48f)
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
    hasStatusRing: Boolean = false
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"
    val ringModifier = if (hasStatusRing) {
        Modifier
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary, SoftBlueTertiary)),
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
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                ),
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
                    .size(180.dp)
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
            Button(onClick = onAction) {
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
        CircularProgressIndicator(strokeWidth = 3.dp)
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
