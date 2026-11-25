package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MessageType {
    TEXT, IMAGE, VIDEO, DOCUMENT, AUDIO, VOICE;
    companion object {
        fun fromRaw(raw: String?): MessageType =
            entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

enum class DeliveryStatus {
    SENT, DELIVERED, READ;
    companion object {
        fun fromRaw(raw: String?): DeliveryStatus =
            entries.firstOrNull { it.name == raw } ?: SENT
    }
}

enum class StatusType {
    TEXT, IMAGE, VIDEO;
    companion object {
        fun fromRaw(raw: String?): StatusType =
            entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

enum class StatusPrivacy {
    ALL_CONTACTS, ONLY_ME;
    companion object {
        fun fromRaw(raw: String?): StatusPrivacy =
            entries.firstOrNull { it.name == raw } ?: ALL_CONTACTS
    }
}

enum class CallType {
    VOICE, VIDEO;
    companion object {
        fun fromRaw(raw: String?): CallType =
            entries.firstOrNull { it.name == raw } ?: VOICE
    }
}

enum class CallStatus {
    RINGING, ACCEPTED, REJECTED, ENDED, MISSED;
    companion object {
        fun fromRaw(raw: String?): CallStatus =
            entries.firstOrNull { it.name == raw } ?: RINGING
    }
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;
    companion object {
        fun fromRaw(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class AppThemeStyle(
    val displayName: String,
    val subtitle: String
) {
    EMERALD("Emerald Forest", "Signature green"),
    OCEAN("Ocean Blue", "Deep sapphire"),
    ROSE("Crimson Rose", "Romantic berry"),
    VIOLET("Royal Violet", "Amethyst plum"),
    SUNSET("Sunset Amber", "Warm terracotta"),
    TEAL("Nordic Teal", "Cool cyan slate"),
    MIDNIGHT("Midnight Gold", "Obsidian & gold");

    companion object {
        fun fromRaw(raw: String?): AppThemeStyle =
            entries.firstOrNull { it.name == raw } ?: EMERALD
    }
}

enum class ContactAddedVia {
    PHONE, QR, DIRECTORY;
    companion object {
        fun fromRaw(raw: String?): ContactAddedVia =
            entries.firstOrNull { it.name == raw } ?: PHONE
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val about: String = "Hey there! I am on Manoj & Sonali.",
    val online: Boolean = false,
    val discoverable: Boolean = true,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPrivateSettings(
    @DocumentId val id: String = "",
    val uid: String = "",
    val phoneNumber: String? = null,
    val countryCode: String = "+91",
    val email: String? = null,
    val fcmToken: String? = null,
    val lastSeenVisible: Boolean = true,
    val onlineVisible: Boolean = true,
    val readReceiptsEnabled: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyGroups: Boolean = true,
    val notifyCalls: Boolean = true,
    val notifyStatus: Boolean = true,
    val darkThemeMode: String = ThemeMode.SYSTEM.name,
    val appThemeStyle: String = AppThemeStyle.EMERALD.name,
    val blockedUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatRoom(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val createdBy: String = "",
    val lastMessage: String = "",
    val lastMessageType: String = MessageType.TEXT.name,
    val lastMessageSenderId: String? = null,
    @ServerTimestamp val lastMessageTime: Timestamp? = null,
    val typingUserIds: List<String> = emptyList(),
    val pinnedBy: List<String> = emptyList(),
    val mutedBy: List<String> = emptyList(),
    val unreadCounts: Map<String, Long> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun peerId(currentUserId: String): String =
        participantIds.firstOrNull { it != currentUserId } ?: ""

    fun unreadFor(userId: String): Int =
        (unreadCounts[userId] as? Number)?.toInt() ?: 0
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String? = null,
    val participantIds: List<String> = emptyList(),
    val type: String = MessageType.TEXT.name,
    val text: String = "",
    val mediaUrl: String? = null,
    val mediaName: String? = null,
    val mediaSizeBytes: Long? = null,
    val voiceDurationMs: Long? = null,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val forwarded: Boolean = false,
    val deliveryStatus: String = DeliveryStatus.SENT.name,
    val reactions: Map<String, String> = emptyMap(),
    val editedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null,
    @ServerTimestamp val timestamp: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val messageType: MessageType get() = MessageType.fromRaw(type)
    val status: DeliveryStatus get() = DeliveryStatus.fromRaw(deliveryStatus)
    val isDeleted: Boolean get() = deletedAt != null
}

@Keep
@IgnoreExtraProperties
data class GroupChat(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String? = null,
    val imageUrl: String? = null,
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val createdBy: String = "",
    val lastMessage: String = "",
    val lastMessageSenderName: String? = null,
    @ServerTimestamp val lastMessageTime: Timestamp? = null,
    val mutedBy: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupMessage(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String? = null,
    val memberIds: List<String> = emptyList(),
    val type: String = MessageType.TEXT.name,
    val text: String = "",
    val mediaUrl: String? = null,
    val mediaName: String? = null,
    val mediaSizeBytes: Long? = null,
    val voiceDurationMs: Long? = null,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val editedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null,
    @ServerTimestamp val timestamp: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val messageType: MessageType get() = MessageType.fromRaw(type)
    val isDeleted: Boolean get() = deletedAt != null
}

@Keep
@IgnoreExtraProperties
data class UserStatus(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val caption: String? = null,
    val mediaUrl: String? = null,
    val type: String = StatusType.TEXT.name,
    val backgroundColorHex: String? = null,
    val privacy: String = StatusPrivacy.ALL_CONTACTS.name,
    val viewerIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusType: StatusType get() = StatusType.fromRaw(type)
    val statusPrivacy: StatusPrivacy get() = StatusPrivacy.fromRaw(privacy)
}

@Keep
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String? = null,
    val callerPhotoUrl: String? = null,
    val receiverId: String = "",
    val receiverName: String? = null,
    val receiverPhotoUrl: String? = null,
    val participantIds: List<String> = emptyList(),
    val callType: String = CallType.VOICE.name,
    val status: String = CallStatus.RINGING.name,
    val offerSdp: String? = null,
    val answerSdp: String? = null,
    val callerCandidates: List<String> = emptyList(),
    val receiverCandidates: List<String> = emptyList(),
    @ServerTimestamp val startedAt: Timestamp? = null,
    val endedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val typeEnum: CallType get() = CallType.fromRaw(callType)
    val statusEnum: CallStatus get() = CallStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class SavedContact(
    @DocumentId val id: String = "",
    val contactUid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val about: String = "",
    val phoneNumber: String? = null,
    val addedVia: String = ContactAddedVia.PHONE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val viaEnum: ContactAddedVia get() = ContactAddedVia.fromRaw(addedVia)
}

@Keep
@IgnoreExtraProperties
data class PhoneLookupEntry(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
