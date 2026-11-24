package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.security.InputValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

sealed interface MediaUploadProgress {
    data class Uploading(val fraction: Float) : MediaUploadProgress
    data class Completed(
        val downloadUrl: String,
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long
    ) : MediaUploadProgress
    data class Failed(val message: String) : MediaUploadProgress
}

class StorageMediaRepository(
    private val appContext: Context,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance().apply {
        maxUploadRetryTimeMillis = 5_000L
        maxOperationRetryTimeMillis = 5_000L
    },
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "StorageMediaRepo"
        private const val AVATAR_INLINE_MAX_BYTES = 52 * 1024
        private const val IMAGE_INLINE_MAX_BYTES = 220 * 1024
        private const val STORAGE_IMAGE_TIMEOUT_MS = 7_500L
    }

    fun uploadUri(
        folder: String,
        uri: Uri,
        isAvatar: Boolean = false
    ): Flow<MediaUploadProgress> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(MediaUploadProgress.Failed("Sign in required before uploading media."))
            return@flow
        }

        val fileName = resolveFileName(uri) ?: "media_${UUID.randomUUID()}"
        val mimeType = resolveMimeType(uri, fileName)
        if (!InputValidator.isAllowedMediaMime(mimeType)) {
            emit(MediaUploadProgress.Failed("Unsupported file format ($mimeType)."))
            return@flow
        }

        val maxBytes = if (isAvatar) InputValidator.MAX_AVATAR_BYTES else InputValidator.MAX_UPLOAD_BYTES
        val rawSize = resolveFileSize(uri)
        if (rawSize > maxBytes && !mimeType.startsWith("image/")) {
            emit(MediaUploadProgress.Failed("File exceeds ${maxBytes / (1024 * 1024)} MB limit."))
            return@flow
        }

        val safeName = InputValidator.sanitizeId(fileName.substringBeforeLast('.')) +
            "_" + UUID.randomUUID().toString().take(8) +
            (fileName.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.let { ".$it" } ?: "")

        val ref = storage.reference.child("$folder/$uid/$safeName")
        val metadata = StorageMetadata.Builder().setContentType(mimeType).build()

        if (mimeType.startsWith("image/") && mimeType != "image/gif") {
            emit(MediaUploadProgress.Uploading(0.2f))
            val initialBytes = compressImageAdaptive(
                uri = uri,
                maxDim = if (isAvatar) 512 else 1280,
                targetMaxBytes = if (isAvatar) AVATAR_INLINE_MAX_BYTES else IMAGE_INLINE_MAX_BYTES
            )
            if (initialBytes == null) {
                emit(MediaUploadProgress.Failed("Could not read selected image."))
                return@flow
            }
            if (initialBytes.size > maxBytes) {
                emit(MediaUploadProgress.Failed("Image exceeds size limit after compression."))
                return@flow
            }

            emit(MediaUploadProgress.Uploading(0.55f))
            val remoteUrl = try {
                withTimeout(STORAGE_IMAGE_TIMEOUT_MS) {
                    ref.putBytes(initialBytes, metadata).await()
                    ref.downloadUrl.await().toString()
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Storage unavailable (${e.message}); using inline compressed image.")
                null
            }

            if (!remoteUrl.isNullOrBlank()) {
                emit(MediaUploadProgress.Uploading(1f))
                emit(
                    MediaUploadProgress.Completed(
                        downloadUrl = remoteUrl,
                        fileName = fileName,
                        mimeType = "image/jpeg",
                        sizeBytes = initialBytes.size.toLong()
                    )
                )
            } else {
                val inlineMax = if (isAvatar) AVATAR_INLINE_MAX_BYTES else IMAGE_INLINE_MAX_BYTES
                val inlineBytes = if (initialBytes.size <= inlineMax) {
                    initialBytes
                } else {
                    compressImageAdaptive(
                        uri = uri,
                        maxDim = if (isAvatar) 360 else 960,
                        targetMaxBytes = inlineMax
                    )
                }

                if (inlineBytes == null || inlineBytes.size > inlineMax) {
                    emit(MediaUploadProgress.Failed("Selected image is too large to save."))
                } else {
                    val dataUri = "data:image/jpeg;base64," +
                        Base64.encodeToString(inlineBytes, Base64.NO_WRAP)
                    emit(MediaUploadProgress.Uploading(1f))
                    emit(
                        MediaUploadProgress.Completed(
                            downloadUrl = dataUri,
                            fileName = fileName,
                            mimeType = "image/jpeg",
                            sizeBytes = inlineBytes.size.toLong()
                        )
                    )
                }
            }
            return@flow
        }

        // Non-image media (video, audio, document) requires Firebase Storage
        try {
            emit(MediaUploadProgress.Uploading(0.25f))
            val snap = ref.putFile(uri, metadata).await()
            val downloadUri = ref.downloadUrl.await()
            emit(MediaUploadProgress.Uploading(1f))
            emit(
                MediaUploadProgress.Completed(
                    downloadUrl = downloadUri.toString(),
                    fileName = fileName,
                    mimeType = mimeType,
                    sizeBytes = snap.totalByteCount.coerceAtLeast(rawSize)
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val friendly = if (mimeType.startsWith("video/")) {
                "Video upload requires Firebase Storage to be enabled in Firebase Console. Photo and text stories work right away."
            } else {
                e.localizedMessage ?: "Upload failed. Verify Firebase Storage is enabled in Firebase Console."
            }
            emit(MediaUploadProgress.Failed(friendly))
        }
    }.flowOn(Dispatchers.IO)

    fun uploadFile(
        folder: String,
        file: File,
        mimeType: String
    ): Flow<MediaUploadProgress> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(MediaUploadProgress.Failed("Sign in required."))
            close()
            return@callbackFlow
        }
        if (!InputValidator.isAllowedMediaMime(mimeType)) {
            trySend(MediaUploadProgress.Failed("Unsupported media type ($mimeType)."))
            close()
            return@callbackFlow
        }
        if (file.length() > InputValidator.MAX_UPLOAD_BYTES) {
            trySend(MediaUploadProgress.Failed("File exceeds 25 MB limit."))
            close()
            return@callbackFlow
        }
        val safeName = InputValidator.sanitizeId(file.nameWithoutExtension) +
            "_" + UUID.randomUUID().toString().take(8) +
            (file.extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: "")
        val ref = storage.reference.child("$folder/$uid/$safeName")
        val metadata = StorageMetadata.Builder().setContentType(mimeType).build()
        val task = ref.putFile(Uri.fromFile(file), metadata)
        task.addOnProgressListener { snap ->
            val total = snap.totalByteCount.coerceAtLeast(1L)
            trySend(MediaUploadProgress.Uploading(snap.bytesTransferred.toFloat() / total.toFloat()))
        }.addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { url ->
                trySend(
                    MediaUploadProgress.Completed(
                        downloadUrl = url.toString(),
                        fileName = file.name,
                        mimeType = mimeType,
                        sizeBytes = file.length()
                    )
                )
                close()
            }.addOnFailureListener { err ->
                trySend(MediaUploadProgress.Failed(err.localizedMessage ?: "Failed to get URL."))
                close()
            }
        }.addOnFailureListener { err ->
            trySend(MediaUploadProgress.Failed(err.localizedMessage ?: "Voice/media upload failed."))
            close()
        }
        awaitClose { if (!task.isComplete) task.cancel() }
    }

    suspend fun downloadToCache(remoteUrl: String, fileName: String): Result<File> = try {
        val dir = File(appContext.cacheDir, "media").apply { mkdirs() }
        val target = File(
            dir,
            InputValidator.sanitizeId(fileName.substringBeforeLast('.')) + "_" +
                System.currentTimeMillis() + "." + fileName.substringAfterLast('.', "bin")
        )
        if (remoteUrl.startsWith("data:", ignoreCase = true) && remoteUrl.contains(",")) {
            val base64 = remoteUrl.substringAfter(",")
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            target.writeBytes(bytes)
        } else {
            storage.getReferenceFromUrl(remoteUrl).getFile(target).await()
        }
        Result.success(target)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun compressImageAdaptive(
        uri: Uri,
        maxDim: Int,
        targetMaxBytes: Int
    ): ByteArray? {
        return try {
            val decoded = decodeSampledBitmap(uri, maxDim) ?: return null
            val rotated = applyExifOrientation(uri, decoded)

            var working = rotated
            var bestBytes: ByteArray? = null

            val dimAttempts = listOf(maxDim, (maxDim * 0.72f).toInt().coerceAtLeast(240))
            for (targetDim in dimAttempts) {
                val ratio = minOf(
                    targetDim.toFloat() / working.width,
                    targetDim.toFloat() / working.height,
                    1f
                )
                val scaled = if (ratio < 1f) {
                    Bitmap.createScaledBitmap(
                        working,
                        (working.width * ratio).toInt().coerceAtLeast(1),
                        (working.height * ratio).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    working
                }

                for (quality in listOf(82, 68, 54, 42)) {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    val candidate = out.toByteArray()
                    bestBytes = candidate
                    if (candidate.size <= targetMaxBytes) {
                        if (scaled !== working) scaled.recycle()
                        if (working !== decoded) working.recycle()
                        decoded.recycle()
                        return candidate
                    }
                }
                if (scaled !== working) scaled.recycle()
            }

            if (working !== decoded) working.recycle()
            decoded.recycle()
            bestBytes
        } catch (e: Exception) {
            Log.e(TAG, "Image compression failed", e)
            null
        }
    }

    private fun decodeSampledBitmap(uri: Uri, maxDim: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        } ?: return null

        var inSampleSize = 1
        val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        if (maxSide > maxDim) {
            val half = maxSide / 2
            while (half / inSampleSize >= maxDim) {
                inSampleSize *= 2
            }
        }
        val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        return appContext.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }

    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun resolveMimeType(uri: Uri, fileName: String): String {
        appContext.contentResolver.getType(uri)?.let { return it }
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.let { return it }
        }
        return "image/jpeg".takeIf { uri.scheme == "file" && ext in listOf("jpg", "jpeg", "png", "webp") }
            ?: "application/octet-stream"
    }

    private fun resolveFileName(uri: Uri): String? {
        if (uri.scheme == "file") return File(uri.path ?: "").name
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    private fun resolveFileSize(uri: Uri): Long {
        if (uri.scheme == "file") return File(uri.path ?: "").length()
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0) return cursor.getLong(idx)
            }
        }
        return 0L
    }
}
