# ElderPal Code Walkthrough

A plain-language guide to where files live and how key features work in the app.

---

## File Structure

### Root & Configuration Files
- **`MainActivity.kt`**: The main starting point of the Android app that sets up the screen navigation.
- **`AndroidManifest.xml`**: Declares app permissions (camera, location, SMS, calls), screens, background services, and system receivers.
- **`app/build.gradle.kts`**: Defines app libraries, Android SDK versions, and injects private API keys into `BuildConfig`.
- **`build.gradle.kts`**: Root project configuration setting up shared plugins.
- **`settings.gradle.kts`**: Configures project repositories and includes the `app` module.
- **`local.properties.example`**: Safe template showing placeholder credential names without storing real keys.

### `ai/` — Artificial Intelligence Helpers
- **`CapturedImageHolder.kt`**: Keeps the captured medicine photo in device memory temporarily without saving it to disk.
- **`MedicineLabelChecker.kt`**: Sends the label photo to the Gemini API, reads the printed text, and checks if it matches today's schedule.

### `data/` — Data Models, Storage & Background Services
- **`AlertMessageBuilder.kt`**: Formats emergency alert text with the date, time, last taken medicine, and a map link.
- **`BootReceiver.kt`**: Restores alarms and restarts the fall detection service when the phone restarts.
- **`Contact.kt`**: Data model representing an emergency family contact.
- **`ContactsRepository.kt`**: Saves and loads emergency family contacts on the phone using Jetpack DataStore.
- **`DemoDataManager.kt`**: Loads realistic fake medicines and contacts for demo testing or clears all data.
- **`DoseLogEntry.kt`**: Data model recording when a dose was taken today.
- **`DoseReminderReceiver.kt`**: Catches scheduled alarm triggers and displays the high-priority reminder alert.
- **`DoseReminderScheduler.kt`**: Schedules, snoozes, and cancels exact medicine reminder alarms via Android's AlarmManager.
- **`EmergencyAlertManager.kt`**: Coordinates emergency actions: gets GPS location, sends SMS alerts, places phone calls, and triggers Twilio voice calls.
- **`FallDetectionPreferences.kt`**: Stores whether background fall detection monitoring is enabled.
- **`FallDetectionService.kt`**: Background service that keeps the accelerometer active to listen for falls when the app is closed.
- **`FallDetector.kt`**: Analyzes motion sensor readings for free-fall and impact patterns indicating a fall.
- **`Medicine.kt`**: Data model for a prescribed medicine, its dose, frequency, and scheduled times.
- **`MedicineRepository.kt`**: Saves medicines, calculates today's doses, and manages taken status on local storage.
- **`PermissionManager.kt`**: Tracks device permission states and initial onboarding progress.

### `screens/` — User Interface Screens & Dialogs
- **`HomeScreen.kt`**: The main dashboard showing the next due medicine, big action buttons, and the SOS button.
- **`ScheduleScreen.kt`**: Shows today's full dose list with statuses (Upcoming, Taken, Missed) and manual log buttons.
- **`AddMedicineScreen.kt`**: Form to type in a new medicine name, dosage, daily times, and food instructions.
- **`ScanPrescriptionScreen.kt`**: Uses the camera and Gemini to read a prescription photo, let the user edit doses, and save them.
- **`CheckMedicineScreen.kt`**: Camera view with framing guides to photograph a medicine label in memory.
- **`CheckMedicineLoadingScreen.kt`**: Animated waiting screen shown while Gemini reads the medicine label.
- **`ResultOkScreen.kt`**: Green screen shown when the photographed medicine is verified and due right now.
- **`ResultNotDueScreen.kt`**: Amber screen shown when a verified medicine is scheduled for later.
- **`ResultNotDueConfirmScreen.kt`**: Safety confirmation required before someone takes a scheduled medicine early.
- **`ResultWrongScreen.kt`**: Red warning screen shown when the photographed medicine is not on the schedule.
- **`ResultUnsureScreen.kt`**: Cautious fallback screen shown when label text is blurry or unreadable.
- **`ReminderScreen.kt`**: Full-screen pop-up alert for a due medicine with options to check label or snooze 10 minutes.
- **`ContactsScreen.kt`**: Lists saved family contacts and allows adding, editing, or previewing alerts.
- **`AddContactScreen.kt`**: Form to enter contact name, relationship, and phone number, with validation against emergency numbers.
- **`AlertPreviewScreen.kt`**: Demonstrates the exact emergency text message family members will receive.
- **`SosScreen.kt`**: Confirmation screen before sending an emergency SOS alert.
- **`FallAlertScreen.kt`**: 10-second audio and visual countdown screen shown when a fall is detected before alerts go out.
- **`EmergencyAlertSentView.kt`**: Confirmation screen displaying delivery statuses for SMS and calls after an alert is sent.
- **`PermissionsScreen.kt`**: Settings screen showing the status of camera, SMS, location, and motion permissions.
- **`FirstRunPermissionsScreen.kt`**: Introductory onboarding walkthrough explaining needed permissions to new users.
- **`DemoToolsScreen.kt`**: Helper screen for hackathon testing to load demo data or simulate a fall instantly.
- **`ElderPalNavHost.kt`**: Master navigation router mapping screen routes and transitions across the app.
- **`InnerScreenHeader.kt` & `PlaceholderScreen.kt`**: Reusable top bar and placeholder screen templates.
- **`*PermissionExplainer.kt`**: Dialogs explaining why Camera, Location, Motion, Notification, Phone Call, or SMS access is needed.

### `ui/theme/` — Styling
- **`Color.kt`**, **`Shape.kt`**, **`Theme.kt`**, **`Type.kt`**: Define high-contrast elder-friendly colors, large typography, and accessible button shapes.

---

## Key Workflows

### 1. How a Medicine Reminder Works
1. A medicine is saved through `AddMedicineScreen` or `ScanPrescriptionScreen` into `MedicineRepository`.
2. `DoseReminderScheduler` computes the next dose timestamp and sets an exact alarm via Android's AlarmManager.
3. At the alarm time, `DoseReminderReceiver` catches the event and triggers a full-screen notification.
4. Tapping the notification displays `ReminderScreen`, where the elder can snooze 10 minutes or tap "Check medicine".
5. If the phone reboots, `BootReceiver` runs automatically to restore and reschedule all alarms.

### 2. How the Label Check Works
1. The elder takes an in-memory photo of their medicine bottle or box on `CheckMedicineScreen`.
2. `CheckMedicineLoadingScreen` calls `MedicineLabelChecker`, which queries Gemini (`gemini-3.5-flash`) with automatic retries.
3. The AI only reads printed text from the image and extracts the drug name; it never diagnoses or decides safety.
4. The app compares the extracted text against the schedule in `MedicineRepository` and checks the current clock time.
5. The app navigates to `ResultOkScreen`, `ResultNotDueScreen`, `ResultWrongScreen`, or `ResultUnsureScreen`.

### 3. How an SOS or Fall Alert Works
1. Triggered manually from `HomeScreen` / `SosScreen`, or detected by `FallDetectionService` and `FallDetector`.
2. For falls, `FallAlertScreen` chimes, vibrates, and gives a 10-second countdown with an "I'm OK" cancel button.
3. On send, `EmergencyAlertManager` fetches GPS location and calls `AlertMessageBuilder` to build the alert text.
4. `EmergencyAlertManager` immediately sends SMS texts to all contacts via Android's `SmsManager`.
5. It then places a standard direct phone call to the primary contact from the elder's phone.
6. In the background, it calls the Twilio REST API to place automated voice calls reading the alert to all contacts.

### 4. Secret Storage & Experimental Features
- Private API keys (`GEMINI_API_KEY`, Twilio credentials) live solely in `local.properties` on the developer's computer.
- `local.properties` is git-ignored and never committed; Gradle injects the keys into `BuildConfig` at build time.
- If Twilio credentials are blank or missing, the voice calling feature stays silently disabled with no crashes.
- Experimental areas: accelerometer-based fall detection heuristics (`FallDetector`) and automated Twilio voice calls.
