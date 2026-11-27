package com.example.notifications

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

internal object NotificationBitmapHelper {

    const val BRAND_EMERALD = 0xFF066046.toInt()
    const val BRAND_TEAL = 0xFF0E8F6A.toInt()
    const val BRAND_ROSE = 0xFFF4176B.toInt()

    private const val AVATAR_SIZE_PX = 192
    private const val CONNECT_TIMEOUT_MS = 2500
    private const val READ_TIMEOUT_MS = 2500

    fun loadCircularAvatarOrMonogram(
        photoUrl: String?,
        displayName: String,
        isCall: Boolean = false
    ): Bitmap {
        val fetched = if (!photoUrl.isNullOrBlank()) {
            downloadBitmap(photoUrl)
        } else {
            null
        }
        return if (fetched != null) {
            createCircularBitmapWithRing(fetched, if (isCall) BRAND_ROSE else BRAND_EMERALD)
        } else {
            createMonogramAvatar(displayName, isCall)
        }
    }

    val cachedBrandBadgeBitmap: Bitmap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createBrandBadgeBitmap()
    }

    fun createBrandBadgeBitmap(): Bitmap {
        val size = AVATAR_SIZE_PX
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val center = size / 2f
        val radius = size / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                BRAND_EMERALD, BRAND_TEAL,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(center, center, radius, bgPaint)

        // Subtle inner highlight ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.035f
            color = Color.argb(55, 255, 255, 255)
        }
        canvas.drawCircle(center, center, radius - ringPaint.strokeWidth, ringPaint)

        // Draw crisp white dual overlapping speech-bubble emblem
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val leftBubble = RectF(
            size * 0.23f,
            size * 0.27f,
            size * 0.65f,
            size * 0.61f
        )
        canvas.drawRoundRect(leftBubble, size * 0.14f, size * 0.14f, bubblePaint)

        val leftTail = Path().apply {
            moveTo(size * 0.30f, size * 0.56f)
            lineTo(size * 0.25f, size * 0.69f)
            lineTo(size * 0.41f, size * 0.59f)
            close()
        }
        canvas.drawPath(leftTail, bubblePaint)

        val rightAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(235, 240, 253, 248)
        }
        val rightOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.03f
            color = BRAND_EMERALD
        }
        val rightBubble = RectF(
            size * 0.42f,
            size * 0.41f,
            size * 0.78f,
            size * 0.71f
        )
        canvas.drawRoundRect(rightBubble, size * 0.12f, size * 0.12f, rightOutlinePaint)
        canvas.drawRoundRect(rightBubble, size * 0.12f, size * 0.12f, rightAccentPaint)

        val rightTail = Path().apply {
            moveTo(size * 0.69f, size * 0.67f)
            lineTo(size * 0.75f, size * 0.77f)
            lineTo(size * 0.59f, size * 0.69f)
            close()
        }
        canvas.drawPath(rightTail, rightAccentPaint)

        return output
    }

    private fun downloadBitmap(rawUrl: String): Bitmap? {
        return try {
            val connection = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
                instanceFollowRedirects = true
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return null
            }
            connection.inputStream.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeStream(stream, null, options)
            }.also {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun createCircularBitmapWithRing(source: Bitmap, ringColor: Int): Bitmap {
        val size = AVATAR_SIZE_PX
        val minEdge = min(source.width, source.height).coerceAtLeast(1)
        val xOffset = (source.width - minEdge) / 2
        val yOffset = (source.height - minEdge) / 2
        val square = Bitmap.createBitmap(source, xOffset, yOffset, minEdge, minEdge)
        val scaled = if (square.width != size) {
            Bitmap.createScaledBitmap(square, size, size, true)
        } else {
            square
        }

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val center = size / 2f
        val stroke = size * 0.045f

        val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(center, center, center - stroke * 0.5f, avatarPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = ringColor
        }
        canvas.drawCircle(center, center, center - stroke * 0.5f, ringPaint)
        return output
    }

    private fun createMonogramAvatar(displayName: String, isCall: Boolean): Bitmap {
        val size = AVATAR_SIZE_PX
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val center = size / 2f

        val startColor = if (isCall) BRAND_ROSE else BRAND_EMERALD
        val endColor = if (isCall) BRAND_EMERALD else BRAND_TEAL

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                startColor, endColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(center, center, center, bgPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.04f
            color = Color.argb(65, 255, 255, 255)
        }
        canvas.drawCircle(center, center, center - ringPaint.strokeWidth * 0.5f, ringPaint)

        val initials = extractInitials(displayName)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val metrics = textPaint.fontMetrics
        val baseline = center - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(initials, center, baseline, textPaint)

        return output
    }

    private fun extractInitials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "MS"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts[0].first()}${parts[1].first()}".uppercase()
        }
    }
}
