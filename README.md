# Manoj & Sonali — Android Messaging Application

**Manoj & Sonali** is a modern, Material 3 Jetpack Compose messaging application built with Kotlin, MVVM + Clean Architecture, and Firebase. It features a custom purple, indigo, and soft-blue visual identity, adaptive light/dark theming, real-time 1-to-1 and group messaging, media and voice sharing, 24-hour status stories, WebRTC voice/video calling with Firestore signaling, and privacy/security controls.

---

## Architecture & Package Structure

```text
app/src/main/java/com/example/
├── ManojSonaliApp.kt              # Application entry point, notification channels & WorkManager setup
├── MainActivity.kt                # Single-activity edge-to-edge host + theme preference observer
├── di/
│   └── AppContainer.kt            # Constructor-injected dependency container (Firebase, Repos, DataStore)
├── domain/model/
│   └── Models.kt                  # Core immutable domain & Firestore models
├── security/
│   ├── FirestoreErrorHandler.kt   # Structured Firestore diagnostics
│   ├── InputValidator.kt          # Length, format, and MIME/size validation
│   └── KeystoreManager.kt         # Android Keystore AES/GCM helper for local secret encryption
├── data/
│   ├── local/PreferencesDataStore.kt
│   ├── remote/StorageMediaRepository.kt
│   └── repository/                # AuthAndUser, Chat, Group, Status, and Call repositories
├── authentication/                # Credential Manager Google Sign-In, Phone Auth OTP, Profile setup
├── presentation/
│   ├── components/                # Brand logo, avatars, shimmer/loading & empty states
│   └── home/                      # Home dashboard (Chats, Groups, Calls, Profile, Search, Status strip)
├── messaging/                     # Real-time 1-to-1 chat, CameraX capture, voice recorder/player
├── groups/                        # Group creation, messaging, admin/member controls
├── status/                        # 24-hour text/photo/video statuses & viewer tracking
├── calls/                         # WebRTC PeerConnection manager + incoming/outgoing/active call UI
├── notifications/                 # FCM service & WorkManager 24h status cleanup worker
├── profile/ & settings/           # Profile editing, theme, notification & privacy settings
└── ui/theme/                      # Color tokens, local Outfit/Plus Jakarta Sans typography, M3 theme
```

---

## Firebase Provisioning & Setup

Firestore and Android app configuration (`app/google-services.json`, `app/src/main/res/values/firebase_applet_config.xml`, `firestore.rules`, `firebase-blueprint.json`, `storage.rules`) are configured for Firebase project `gen-lang-client-0701103536` and database `ai-studio-android-manojson-95bc54c9-fe34-4aeb-8012-4f1a2c0d3885`.

### Required Firebase Console Configuration
Before testing SMS OTP, media uploads, and remote push notifications on a physical device or emulator with Google Play Services, enable the following in the **Firebase Console** (`gen-lang-client-0701103536`):

1. **Authentication → Sign-in method & Project Settings**:
   - **Google**: Enabled for instant sign-in via Jetpack Credential Manager.
   - **Phone**: Enable the **Phone** provider (and add **Phone numbers for testing** for emulator development, since Android emulators without an updated Google Play Store hit Play Integrity error `-9` `CANNOT_BIND_TO_SERVICE`).
   - **SHA-1 / SHA-256 & reCAPTCHA Enterprise**: Ensure both **SHA-1** and **SHA-256** signing fingerprints are registered under **Project Settings → General → Your apps**, and enable **Play Integrity API** (and **reCAPTCHA Enterprise API** with an Android site key if using reCAPTCHA Enterprise enforcement) in Google Cloud Console for `gen-lang-client-0701103536`.
2. **Firebase Storage**:
   - Initialize the default Storage bucket (`gen-lang-client-0701103536.firebasestorage.app`) in Firebase Console.
   - Deploy `storage.rules` from the project root (`firebase deploy --only storage`) to enforce authenticated uploads, 25 MB size limits, and allowed MIME types.
3. **Cloud Messaging (FCM)**:
   - Token registration (`users/{userId}/private/settings.fcmToken`) runs automatically in `ManojSonaliMessagingService`.
   - Remote push delivery for offline messages and incoming calls requires a backend trigger (e.g., Cloud Functions for Firebase listening to `chats/{chatId}/messages`, `groups/{groupId}/messages`, and `calls/{callId}`) using the Firebase Admin SDK.

---

## Testing Instructions

### 1. Build Verification (JVM / Gradle)
```bash
gradle assembleDebug
gradle :app:testDebugUnitTest
```
*(In this cloud environment, use `compile_applet` to build and verify the APK.)*

### 2. Firestore Security Rules Verification
`firestore.test.js` contains rules tests covering default-deny, PII isolation (`/users/{uid}/private`), 1-to-1 chat participant access, group admin/member permissions, status privacy, and call signaling transitions.

With the Firebase Local Emulator Suite and Node.js installed locally:
```bash
firebase emulators:exec --only firestore --project test-project \
  'FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js'
```

---

## Security Architecture & Known Limitations

- **Zero-Trust Firestore Rules**: Global default-deny (`allow read, write: if false`) with per-collection field allowlists, immutable ownership fields, server timestamp checks, and denormalized participant arrays on message subcollections.
- **PII Separation**: Phone numbers, emails, FCM tokens, blocked user IDs, and privacy preferences live under `/users/{userId}/private/settings` (owner-only read/write), keeping `/users/{userId}` free of sensitive PII.
- **Transport & Local Protection**: Network communication uses Firebase TLS transport encryption and Android Keystore (`AES/GCM/NoPadding`) for local preference protection. **End-to-End Encryption (E2EE) is not implemented** (no Signal-protocol key exchange is performed between devices).
- **WebRTC TURN/NAT Traversal**: `WebRtcCallManager` configures Google STUN servers (`stun:stun.l.google.com:19302`) and exchanges SDP/ICE via Firestore (`calls/{callId}`). Calls behind strict symmetric NATs or corporate firewalls require adding a dedicated TURN server in `WebRtcCallManager`.
- **SMS Phone Auth & Play Services**: Firebase Phone Authentication SMS delivery requires a real device or Google Play-enabled emulator with Firebase Console Phone Auth active; Credential Manager Google Sign-In is available alongside it.
