# CondoSuper Android App

This is the Android version of the CondoSuper iOS app, converted from SwiftUI to Jetpack Compose.

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/condosuper/app/
│   │   │   ├── data/models/          # Data models
│   │   │   ├── managers/             # Business logic managers
│   │   │   ├── ui/
│   │   │   │   ├── screens/          # UI screens
│   │   │   │   ├── theme/           # Theme and colors
│   │   │   │   └── navigation/      # Navigation setup
│   │   │   └── MainActivity.kt       # Main activity
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## Key Conversions

### iOS → Android Equivalents

1. **SwiftUI → Jetpack Compose**
   - `@State` → `remember { mutableStateOf() }`
   - `@StateObject` → `viewModel()` or `collectAsState()`
   - `@Published` → `StateFlow` / `MutableStateFlow`
   - `ObservableObject` → `ViewModel` or StateFlow

2. **Firebase**
   - iOS: `Firestore.firestore()` → Android: `FirebaseFirestore.getInstance()`
   - iOS: `Storage.storage()` → Android: `FirebaseStorage.getInstance()`
   - Listeners work similarly with `addSnapshotListener`

3. **Location Services**
   - iOS: `CLLocationManager` → Android: `FusedLocationProviderClient`
   - Permissions handled via `ActivityResultLauncher`

4. **Encryption**
   - iOS: `CryptoKit` → Android: `javax.crypto` + `EncryptedSharedPreferences`
   - AES-GCM encryption maintained

5. **Biometric Auth**
   - iOS: `LocalAuthentication` → Android: `BiometricPrompt`
   - Face ID / Touch ID → Fingerprint / Face Unlock

6. **Notifications**
   - iOS: `UNUserNotificationCenter` → Android: `NotificationManagerCompat`
   - Push notifications via FCM (Firebase Cloud Messaging)

7. **PDF Generation**
   - iOS: `UIGraphicsPDFRenderer` → Android: `iText7` library
   - Similar PDF creation workflow

8. **Image Loading**
   - iOS: `AsyncImage` → Android: `Coil` library
   - Similar async image loading

## Setup Instructions

1. **Add Firebase Configuration**
   - Place `google-services.json` in `app/` directory
   - Get it from Firebase Console

2. **Configure Permissions**
   - All permissions are declared in `AndroidManifest.xml`
   - Runtime permissions handled in screens

3. **Build the Project**
   ```bash
   ./gradlew build
   ```

4. **Run the App**
   ```bash
   ./gradlew installDebug
   ```

## Remaining Work

The following screens need to be fully implemented:

1. **SplashScreen** - Initial loading screen
2. **AuthWelcomeScreen** - Welcome/auth selection
3. **CompanySetupScreen** - Company registration
4. **CompanyLoginScreen** - Company login
5. **EmployeeLoginScreen** - Employee login
6. **MainTabScreen** - Main navigation with tabs
7. **TimeClockView** - Time tracking screen
8. **PhotosView** - Photo gallery/feed
9. **DashboardView** - Manager dashboard
10. **ReportsView** - PDF report generation
11. **ManageView** - Employee/site management
12. **TasksView** - Task management
13. **MessengerView** - Messaging interface
14. **ProfileView** - User profile

## Key Features Implemented

✅ Data models converted
✅ Firebase manager with real-time listeners
✅ Location manager with GPS tracking
✅ Encryption manager with AES-GCM
✅ Biometric authentication
✅ Network monitoring
✅ Notification manager
✅ Upload queue manager for offline support

## Notes

- All timestamps use `Long` (milliseconds since epoch) instead of `Date`
- UUIDs are stored as `String` instead of `UUID` type
- State management uses Kotlin Flow instead of Combine
- Navigation uses Jetpack Navigation Compose
- UI uses Material 3 design system

## Dependencies

Key dependencies:
- Jetpack Compose for UI
- Firebase (Firestore, Storage, Auth)
- Google Play Services (Location, Maps)
- Coil for image loading
- iText7 for PDF generation
- Kotlin Coroutines for async operations


