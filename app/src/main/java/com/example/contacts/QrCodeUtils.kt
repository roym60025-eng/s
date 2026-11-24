package com.example.contacts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors

object QrCodeUtils {

    private const val MAX_GALLERY_DECODE_DIMENSION = 1280

    private val decodeExecutor = Executors.newSingleThreadExecutor()

    private val decodeHints: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true
    )

    fun generateQrImageBitmap(
        content: String,
        sizePx: Int = 640,
        foregroundColor: Int = Color.rgb(40, 29, 90), // Deep indigo matching brand
        backgroundColor: Int = Color.WHITE
    ): ImageBitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix.get(x, y)) foregroundColor else backgroundColor
            }
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        return bitmap.asImageBitmap()
    }

    fun decodeQrFromYuvImageProxy(imageProxy: ImageProxy): String? {
        val plane = imageProxy.planes.firstOrNull() ?: return null
        val width = imageProxy.width
        val height = imageProxy.height
        if (width <= 0 || height <= 0) return null

        val yBytes = extractLuminanceBytes(
            plane = plane,
            width = width,
            height = height
        )
        val source = PlanarYUVLuminanceSource(
            yBytes,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        return decodeLuminanceSource(source)
    }

    fun scanQrFromImageUri(
        context: Context,
        uri: Uri,
        onSuccess: (String?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val appContext = context.applicationContext
        val mainExecutor = ContextCompat.getMainExecutor(appContext)
        decodeExecutor.execute {
            try {
                val bitmap = decodeSampledBitmap(appContext, uri)
                    ?: throw IllegalStateException("Unable to read selected image.")
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                bitmap.recycle()

                val source = RGBLuminanceSource(width, height, pixels)
                val decoded = decodeLuminanceSource(source)
                mainExecutor.execute { onSuccess(decoded) }
            } catch (e: Exception) {
                mainExecutor.execute { onError(e) }
            }
        }
    }

    private fun extractLuminanceBytes(
        plane: ImageProxy.PlaneProxy,
        width: Int,
        height: Int
    ): ByteArray {
        val buffer = plane.buffer
        buffer.rewind()
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val data = ByteArray(width * height)

        if (pixelStride == 1 && rowStride == width && buffer.remaining() >= data.size) {
            buffer.get(data, 0, data.size)
            return data
        }

        val rowBuffer = ByteArray(rowStride)
        for (y in 0 until height) {
            val remaining = buffer.remaining()
            if (remaining <= 0) break
            val toRead = minOf(rowStride, remaining)
            buffer.get(rowBuffer, 0, toRead)
            val destOffset = y * width
            if (pixelStride == 1) {
                System.arraycopy(rowBuffer, 0, data, destOffset, minOf(width, toRead))
            } else {
                for (x in 0 until width) {
                    val srcIndex = x * pixelStride
                    if (srcIndex < toRead) {
                        data[destOffset + x] = rowBuffer[srcIndex]
                    }
                }
            }
        }
        return data
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        val sampleSize = calculateInSampleSize(
            width = boundsOptions.outWidth,
            height = boundsOptions.outHeight,
            maxDimension = MAX_GALLERY_DECODE_DIMENSION
        )
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        if (width > maxDimension || height > maxDimension) {
            var halfWidth = width / 2
            var halfHeight = height / 2
            while ((halfWidth / inSampleSize) >= maxDimension || (halfHeight / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun decodeLuminanceSource(source: LuminanceSource): String? {
        val reader = QRCodeReader()
        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), decodeHints)
                .text
                ?.takeIf { it.isNotBlank() }
        } catch (_: ReaderException) {
            try {
                reader.reset()
                reader.decode(BinaryBitmap(HybridBinarizer(source.invert())), decodeHints)
                    .text
                    ?.takeIf { it.isNotBlank() }
            } catch (_: ReaderException) {
                null
            }
        } finally {
            reader.reset()
        }
    }
}
