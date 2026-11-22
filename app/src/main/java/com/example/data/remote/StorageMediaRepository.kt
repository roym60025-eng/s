package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.example.security.InputValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun uploadUri(
        folder: String,
        uri: Uri,
        isAvatar: Boolean = false
    ): Flow<MediaUploadProgress> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(MediaUploadProgress.Failed("Sign in required before uploading media."))
            close()
            return@callbackFlow
        }

        val mimeType = appContext.contentResolver.getType(uri) ?: "application/octet-stream"
        if (!InputValidator.isAllowedMediaMime(mimeType)) {
            trySend(MediaUploadProgress.Failed("Unsupported file format ($mimeType)."))
            close()
            return@callbackFlow
        }

        val fileName = resolveFileName(uri) ?: "media_${UUID.randomUUID()}"
        val maxBytes = if (isAvatar) InputValidator.MAX_AVATAR_BYTES else InputValidator.MAX_UPLOAD_BYTES
        val rawSize = resolveFileSize(uri)
        if (rawSize > maxBytes && !mimeType.startsWith("image/")) {
            trySend(MediaUploadProgress.Failed("File exceeds ${maxBytes / (1024 * 1024)} MB limit."))
            close()
            return@callbackFlow
        }

        val safeName = InputValidator.sanitizeId(fileName.substringBeforeLast('.')) +
            "_" + UUID.randomUUID().toString().take(8) +
            (fileName.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.let { ".$it" } ?: "")

        val ref = storage.reference.child("$folder/$uid/$safeName")
        val metadata = StorageMetadata.Builder().setContentType(mimeType).build()

        val task = if (mimeType.startsWith("image/") && mimeType != "image/gif") {
            val compressedBytes = compressImage(uri, maxDim = if (isAvatar) 720 else 1600)
            if (compressedBytes == null) {
                trySend(MediaUploadProgress.Failed("Could not read selected image."))
                close()
                return@callbackFlow
            }
            if (compressedBytes.size > maxBytes) {
                trySend(MediaUploadProgress.Failed("Image exceeds size limit after compression."))
                close()
                return@callbackFlow
            }
            ref.putBytes(compressedBytes, metadata)
        } else {
            ref.putFile(uri, metadata)
        }

        task.addOnProgressListener { snap ->
            val total = snap.totalByteCount.coerceAtLeast(1L)
            val fraction = (snap.bytesTransferred.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            trySend(MediaUploadProgress.Uploading(fraction))
        }.addOnSuccessListener { snap ->
            ref.downloadUrl.addOnSuccessListener { downloadUri ->
                trySend(
                    MediaUploadProgress.Completed(
                        downloadUrl = downloadUri.toString(),
                        fileName = fileName,
                        mimeType = mimeType,
                        sizeBytes = snap.totalByteCount.coerceAtLeast(rawSize)
                    )
                )
                close()
            }.addOnFailureListener { err ->
                trySend(MediaUploadProgress.Failed(err.localizedMessage ?: "Failed to resolve media URL."))
                close()
            }
        }.addOnFailureListener { err ->
            trySend(
                MediaUploadProgress.Failed(
                    err.localizedMessage ?: "Upload failed. Verify Firebase Storage is enabled in Firebase Console."
                )
            )
            close()
        }

        awaitClose {
            if (!task.isComplete) {
                task.cancel()
            }
        }
    }

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
        val target = File(dir, InputValidator.sanitizeId(fileName.substringBeforeLast('.')) + "_" + System.currentTimeMillis() + "." + fileName.substringAfterLast('.', "bin"))
        storage.getReferenceFromUrl(remoteUrl).getFile(target).await()
        Result.success(target)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun compressImage(uri: Uri, maxDim: Int): ByteArray? {
        return try {
            val input = appContext.contentResolver.openInputStream(uri) ?: return null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return null
            val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * ratio).toInt().coerceAtLeast(1),
                    (original.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                original
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            if (scaled !== original) scaled.recycle()
            original.recycle()
            out.toByteArray()
        } catch (_: Exception) {
            null
        }
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
