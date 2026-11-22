package com.example.security

object InputValidator {
    const val MAX_DISPLAY_NAME = 60
    const val MAX_ABOUT = 160
    const val MAX_MESSAGE_TEXT = 4000
    const val MAX_GROUP_NAME = 80
    const val MAX_GROUP_DESC = 300
    const val MAX_STATUS_CAPTION = 500
    const val MAX_UPLOAD_BYTES = 25L * 1024L * 1024L // 25 MB
    const val MAX_AVATAR_BYTES = 10L * 1024L * 1024L // 10 MB

    private val allowedMimePrefixes = listOf("image/", "video/", "audio/")
    private val allowedDocMimes = setOf(
        "application/pdf",
        "text/plain",
        "application/msword",
        "application/zip"
    )

    fun isAllowedMediaMime(mimeType: String?): Boolean {
        if (mimeType.isNullOrBlank()) return false
        return allowedMimePrefixes.any { mimeType.startsWith(it) } ||
            mimeType.startsWith("application/vnd.") ||
            mimeType in allowedDocMimes
    }

    fun validateDisplayName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Display name is required."
            trimmed.length > MAX_DISPLAY_NAME -> "Display name must be $MAX_DISPLAY_NAME characters or less."
            else -> null
        }
    }

    fun validateGroupName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Group name is required."
            trimmed.length > MAX_GROUP_NAME -> "Group name must be $MAX_GROUP_NAME characters or less."
            else -> null
        }
    }

    fun validatePhoneNumber(dialCode: String, localNumber: String): String? {
        val digits = localNumber.filter { it.isDigit() }
        return when {
            digits.length < 6 -> "Enter a valid phone number."
            digits.length > 15 -> "Phone number is too long."
            !dialCode.startsWith("+") -> "Select a valid country code."
            else -> null
        }
    }

    fun sanitizeId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)

    fun nonNullFields(vararg pairs: Pair<String, Any?>): Map<String, Any> = buildMap {
        for ((key, value) in pairs) {
            if (value != null) put(key, value)
        }
    }

    fun phoneLookupKey(dialCode: String, localOrFullNumber: String): String? {
        val trimmed = localOrFullNumber.trim()
        val rawDigits = trimmed.filter { it.isDigit() }
        if (rawDigits.length < 6) return null
        val key = if (trimmed.startsWith("+")) {
            rawDigits
        } else {
            val codeDigits = dialCode.filter { it.isDigit() }
            if (codeDigits.isNotEmpty() && !rawDigits.startsWith(codeDigits)) {
                codeDigits + rawDigits
            } else {
                rawDigits
            }
        }
        return if (key.length in 6..15 && key.all { it.isDigit() }) key else null
    }

    fun buildContactQrPayload(uid: String): String = "manojsonali://contact?uid=$uid"

    sealed interface ScannedContactTarget {
        data class ByUid(val uid: String) : ScannedContactTarget
        data class ByPhone(val rawPhone: String) : ScannedContactTarget
    }

    fun parseContactQrPayload(raw: String): ScannedContactTarget? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed.startsWith("manojsonali://", ignoreCase = true)) {
            val uidParam = android.net.Uri.parse(trimmed).getQueryParameter("uid")?.trim()
            if (!uidParam.isNullOrBlank() && uidParam.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
                return ScannedContactTarget.ByUid(uidParam)
            }
        }
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val phone = trimmed.removePrefix("tel:").removePrefix("TEL:").trim()
            if (phone.filter { it.isDigit() }.length in 6..15) {
                return ScannedContactTarget.ByPhone(phone)
            }
        }
        if (trimmed.matches(Regex("^\\+?[0-9\\s\\-()]{6,20}$"))) {
            val digits = trimmed.filter { it.isDigit() }
            if (digits.length in 6..15) {
                return ScannedContactTarget.ByPhone(trimmed)
            }
        }
        if (trimmed.matches(Regex("^[a-zA-Z0-9_\\-]{12,128}$"))) {
            return ScannedContactTarget.ByUid(trimmed)
        }
        return null
    }
}
