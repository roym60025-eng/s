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
}
