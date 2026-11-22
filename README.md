# ElderPal

> A calm, high-contrast Android safety assistant designed specifically for elderly individuals to prevent medication errors, detect accidental falls, and keep family informed in emergencies.

ElderPal addresses two of the most critical threats to the independence and safety of older adults living alone: accidental medication errors and unassisted falls. For seniors experiencing reduced visual acuity, declining memory, or mild tremor, identifying similar-looking pill packages and remembering strict schedules can be stressful and error-prone. At the same time, a sudden fall can leave a person unable to reach a phone to call for help. ElderPal bridges this gap with an accessible, high-contrast interface: it verifies medicine labels before ingestion using Google Gemini multimodal AI, monitors motion sensors in the background for falls, and automatically dispatches emergency alerts with precise Google Maps locations and recent medication history to family contacts.

---

## Features

- **Medicine Reminders with Snooze**: Timed, high-priority notifications at scheduled dose times with a prominent 10-minute snooze option.
- **Medicine Schedule & Dose Log**: Clear overview of today's doses (upcoming, taken, or missed), backed by a persistent local dose log.
- **Medicine Label Check with Gemini**: Real-time camera verification of pill containers against the current schedule using Google's Gemini multimodal AI before taking any pill.
- **Prescription Scanning with Gemini**: Capture a prescription photo to extract medication names, doses, and frequencies into a proposed schedule for the user to review and confirm before anything is saved.
- **Fall Detection with Cancel Countdown**: Continuous background accelerometer monitoring that detects free-fall followed by impact and stillness, initiating a 10-second cancel countdown with audible and visual alerts.
- **Always-Accessible SOS**: Large, high-visibility emergency button with a dedicated confirmation step to prevent accidental triggers.
- **Comprehensive Alert Texts**: Dispatches SMS messages to all emergency contacts containing the incident type, exact time, Google Maps location link, and the most recent medication taken.
- **Optional Spoken Twilio Voice Calls**: Optional automated phone call to saved family contacts using Twilio text-to-speech to read the emergency alert message aloud.
- **Emergency Contacts Management**: Manage family contacts with customizable relationship tags and a dedicated "Call First" priority contact.
- **First-Run Permissions Flow**: Guided step-by-step introduction explaining the necessity of camera, notifications, SMS, location, motion, and phone permissions before asking the Android system.
- **Demo Tools Area for Judges**: Dedicated testing suite allowing evaluators to load realistic sample schedules, test SIM alerts safely, inspect live sensor telemetry, and simulate falls on demand.

---

## How It Works

### 1. Medicine Label Check
When the user photographs a medicine container, the image is passed to the Gemini API (`gemini-3.5-flash`) along with the user's scheduled medication list and the current local time. Crucially, the AI is tasked solely with reading and transcribing the printed text on the packaging into structured data. The ElderPal Android app itself evaluates the transcribed label against the schedule logic and time windows (±60 minutes):
- **OK (Green)**: The medicine is on the list and due right now. Allows "Mark as taken".
- **WRONG (Red)**: The detected medicine is not part of the active prescription list. Warns the user and offers a one-tap button to call family.
- **NOT DUE (Amber)**: The medicine belongs to the user but is not due yet. Allows waiting or requires a deliberate second confirmation before taking early.
- **UNSURE (Grey)**: The label is obscured, blurry, low-confidence, or the device is offline. Instructs the user to seek help from a pharmacist or family member.

ElderPal never shows "All good" unless its internal verification logic strictly agrees with the schedule.

### 2. Prescription Scanning
Users can photograph a paper prescription or doctor's note. The image is sent to Gemini to identify drug names, dosages, and dosing frequencies. Gemini proposes a structured medication schedule. **Nothing is saved automatically**: the user is presented with an editable review screen where each extracted medicine can be inspected, adjusted, or discarded before being written to local storage.

### 3. Emergency Alert System (SOS & Falls)
When an emergency is confirmed (either via the SOS button or an uncancelled fall countdown), the `EmergencyAlertManager` executes a coordinated alert sequence:
1. **Emergency SMS**: Sends an SMS to all saved family contacts containing the event type, exact timestamp, the elder's last recorded dose from the dose log, and a Google Maps location link (`https://maps.google.com/?q=lat,lng`).
2. **Direct Phone Call**: Initiates an immediate direct cellular phone call to the contact designated as "Call First". If the phone-call permission was denied, it opens the phone dialer with the number pre-filled.
3. **Optional Twilio Voice Call**: If Twilio credentials are configured, an asynchronous HTTP request instructs Twilio to place outbound phone calls to all saved contacts and recite the alert text aloud.

### 4. Sensor-Based Fall Detection
The background service monitors the device's accelerometer at high frequency (`SENSOR_DELAY_GAME`). It employs an explainable, multi-stage state machine:
- **Free-Fall**: Senses a sudden drop where total acceleration drops below near-weightlessness (~0.5g).
- **Impact**: Detects a sharp deceleration spike (~1.8g) within 1.5 seconds of free-fall.
- **Inactivity / Stillness**: Requires a short post-impact stillness window (~1 second) to distinguish falls from active drops (like catching the phone or tossing it onto a couch).
- **Cancel Countdown**: Once triggered, a full-screen alert turns the screen on, wakes the device over the lock screen, and sounds an alarm with a 10-second countdown. If the user taps "I'm OK", the alert is immediately canceled and no messages or calls are sent.

---

## Setup Instructions for Judges

### Requirements
- **Hardware**: A physical Android smartphone running Android 8.0 (API 26) or higher. *(Note: Android 10+ recommended; testing was performed on OnePlus Nord CE2 / Android 13).*
- **Software**: Android Studio (Ladybug / Jellyfish or newer) with JDK 11+, and an active Internet connection on the phone for Gemini AI checks.
- **SIM Card**: An active SIM card with SMS and voice calling balance if evaluating live cellular alerts.

### 1. Clone the Repository
```bash
git clone <repository-url>
cd evo
```

### 2. Configure Local Properties & API Keys
Create your `local.properties` file in the project root by copying `local.properties.example`:
```bash
cp local.properties.example local.properties
```

Open `local.properties` and add your Android SDK path and Gemini API key:
```properties
sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=AIzaSy...your_gemini_api_key_here
```

> **Where to get a free Gemini API Key:**
> 1. Visit [Google AI Studio](https://aistudio.google.com/).
> 2. Sign in with your Google account.
> 3. Click **"Get API key"** and create a free API key.
> 4. Paste the key into `local.properties` as `GEMINI_API_KEY`.

**Gemini Model Used:**
The app uses **`gemini-3.5-flash`** (configured as `GEMINI_MODEL_NAME` in `com.example.elderpal.ai.MedicineLabelChecker`).

### 3. Build & Run on an Android Phone
1. Connect your physical Android phone via USB and enable **USB Debugging** in Developer Options.
2. Build and install via Gradle or Android Studio:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```
   *(On Windows Command Prompt or PowerShell, use `.\gradlew.bat assembleDebug`)*

### 4. Permissions & First-Run Flow
Upon opening ElderPal for the first time, a step-by-step introduction introduces the necessary permissions with plain-language explanations:
- **Notifications**: To deliver audible medication reminders at dose times.
- **Camera**: To photograph pill bottles and paper prescriptions.
- **SMS (`SEND_SMS`)**: To dispatch emergency alerts with location to family.
- **Location (`ACCESS_FINE_LOCATION`)**: To include a Google Maps link in emergency alerts.
- **Motion (`HIGH_SAMPLING_RATE_SENSORS`)**: To monitor the accelerometer for fall detection.
- **Phone Calls (`CALL_PHONE`)**: To directly dial the primary family contact during an emergency.

Tap **"Allow"** on each explainer to trigger the Android system dialog.

### 5. Using Demo Mode (Judges' Guide)
To facilitate rapid testing without needing physical pills or risking false alerts to strangers:
1. On the **Home screen**, tap the small teal **heart logo** at the top center.
2. This opens the **Permissions** screen. Scroll to the footer and tap **"Demo tools (judges only)"**. *(Alternatively, tapping the heart logo on Home gives direct access).*
3. **Load Demo Data**:
   - Tap **"Load demo data"**.
   - A dialog asks for an emergency contact phone number. **Enter your own mobile phone number** so test SMS messages and calls come to your device and never disturb strangers.
   - This populates three sample medications ("Sample Medicine A", "Sample Medicine B", "Sample Medicine C") scheduled around the current time (one just passed, one due now, one upcoming), logs a past dose, and schedules reminder alarms.
4. **Simulate Fall**:
   - In Demo Tools, tap **"Simulate fall (Test)"**.
   - This immediately launches the full-screen "Are you okay?" 10-second countdown without needing to physically drop the phone. You can test canceling it or letting it expire to verify the SMS dispatch.
5. **Sensor Test Readout**:
   - In Demo Tools, view the live **"Sensor test"** section.
   - Shows real-time acceleration in g, minimum/maximum values over the past 2 seconds, and the current state of the fall detector state machine (*Waiting*, *Free-fall seen*, *Impact seen*, etc.).
6. **Reset Data**:
   - Tap **"Reset demo data"** at any time to wipe the schedule and contacts back to a clean slate.

---

### Important Hardware & Device Notes

- **Emulators**: An Android emulator cannot simulate real physical falls, lacks a cellular SIM to send real SMS or place real phone calls, and provides a low-fidelity virtual camera that makes pill label OCR testing unreliable. **Please evaluate on a physical device.**
- **SMS & Carrier Balance**: Live SMS alerts and phone calls utilize standard cellular carrier services and will deduct from the device's SIM balance or carrier plan.
- **Device Battery Optimization (OnePlus / Xiaomi / Samsung)**: On devices with aggressive background app killers (e.g., OxygenOS / ColorOS), navigate to Android Settings > Apps > ElderPal > Battery Usage and set it to **"Unrestricted"**. This ensures the fall detection background service is not killed by the operating system when the screen is turned off.

---

### Optional: Twilio Voice Call Setup

ElderPal supports automated voice calls via Twilio. This feature is completely optional:
1. Provide the following keys in `local.properties`:
   ```properties
   TWILIO_ACCOUNT_SID=ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   TWILIO_AUTH_TOKEN=your_auth_token_here
   TWILIO_FROM_NUMBER=+15551234567
   ```
2. **Twilio Trial Account Note**: If using a free Twilio trial account:
   - Outbound calls can **only be placed to verified phone numbers** (verified in your Twilio Console under *Phone Numbers > Verified Caller IDs*).
   - Twilio trial calls prepend a mandatory trial audio disclaimer before reading the message.
   - Outbound international voice permissions (e.g., India +91) must be manually enabled in your Twilio Voice Geo-Permissions console.
3. **Graceful Fallback**: If these properties are empty or omitted, **the Twilio feature is silently disabled**. All other features (SMS, cellular direct calling, Gemini checks, and reminders) function normally without error.

---

## Built During the Hackathon vs Pre-Existing

**Everything in this repository was created from scratch during the hackathon.**

- **Pre-existing templates**: The standard Android Studio "Empty Activity" project template (Jetpack Compose, Kotlin 2.2).
- **Pre-existing assets**: Open-source typography files imported from Google Fonts (`Atkinson Hyperlegible` and `Patrick Hand`). All vector drawables, layouts, background services, sensor algorithms, AI prompts, and business logic were conceived and authored entirely during the event.

---

## Credits & Open-Source Licenses

### Technologies & Assistants
- **Google Antigravity**: AI coding agent that assisted in implementing Kotlin Jetpack Compose screens, sensor logic, and debugging Android background services.
- **Google Gemini API**: Multimodal vision AI providing label reading and prescription text parsing.
- **Claude Design**: UI/UX design specifications, wireframes, and interface layouts in `/design`.
- **Twilio**: Optional programmable voice calling infrastructure.

### Design Note on Phone Call Permission
The permission explainer for phone calls (`PhoneCallPermissionExplainer`) was designed in-house to strictly match the layout, typography, colors, and button sizing of the official design mockups (`permissions-location.png`, `permissions-motion.png`, `permissions-sms.png`), as no specific graphic was included in the starter design assets.

### Open-Source Libraries
All dependencies are distributed under open-source licenses:

| Library | Version | License |
|:---|:---|:---|
| **Android Gradle Plugin** (`com.android.application`) | `9.4.1` | Apache License 2.0 |
| **Kotlin Compose Compiler Plugin** (`org.jetbrains.kotlin.plugin.compose`) | `2.2.10` | Apache License 2.0 |
| **Google Generative AI SDK** (`com.google.ai.client.generativeai:generativeai`) | `0.9.0` | Apache License 2.0 |
| **AndroidX Compose BOM** (`androidx.compose:compose-bom`) | `2026.02.01` | Apache License 2.0 |
| **AndroidX Navigation Compose** (`androidx.navigation:navigation-compose`) | `2.8.8` | Apache License 2.0 |
| **AndroidX DataStore Preferences** (`androidx.datastore:datastore-preferences`) | `1.1.3` | Apache License 2.0 |
| **AndroidX CameraX** (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) | `1.7.0` | Apache License 2.0 |
| **AndroidX Core KTX** (`androidx.core:core-ktx`) | `1.19.1` | Apache License 2.0 |
| **AndroidX Lifecycle Runtime KTX** (`androidx.lifecycle:lifecycle-runtime-ktx`) | `2.11.1` | Apache License 2.0 |
| **AndroidX Activity Compose** (`androidx.activity:activity-compose`) | `1.13.0` | Apache License 2.0 |
| **JUnit** (`junit:junit`) | `4.13.2` | Eclipse Public License (EPL) 1.0 |
| **AndroidX JUnit Extension** (`androidx.test.ext:junit`) | `1.3.0` | Apache License 2.0 |
| **AndroidX Espresso Core** (`androidx.test.espresso:espresso-core`) | `3.7.0` | Apache License 2.0 |

### Fonts
- **Atkinson Hyperlegible** (`atkinson_hyperlegible_regular.ttf`, `atkinson_hyperlegible_bold.ttf`):
  - *Source*: [Google Fonts / Braille Institute](https://fonts.google.com/specimen/Atkinson+Hyperlegible)
  - *License*: [SIL Open Font License (OFL) 1.1](https://openfontlicense.org/)
  - Engineered specifically with distinctive character shapes to increase legibility for readers with low vision.
- **Patrick Hand** (`patrick_hand_regular.ttf`):
  - *Source*: [Google Fonts / Rodrigo Fuenzalida](https://fonts.google.com/specimen/Patrick+Hand)
  - *License*: [SIL Open Font License (OFL) 1.1](https://openfontlicense.org/)
  - Used sparingly for the friendly handwritten greeting on the home screen.

---

## Safety & Limitations

- **Assistive Helper, Not Medical Advice**: ElderPal is an assistive technology tool designed to aid memory and routine. It is **not** a certified medical device, diagnostic tool, or substitute for professional medical advice. Users must always verify dosages and prescriptions with a doctor or licensed pharmacist.
- **AI Limitations & Network Dependency**: The Gemini label check and prescription scan rely on cloud AI. They require an active Internet connection and can be affected by server load, low lighting, glare, or damaged packaging. If the model is unsure, it explicitly prompts the user to verify manually.
- **Google Play Restricted Permissions**: Permissions such as `SEND_SMS` and `CALL_PHONE` are strictly controlled under Google Play's Sensitive Permissions Policy. ElderPal is packaged as an educational hackathon prototype APK and is not currently published on the Google Play Store.
- **Fall Detection Constraints**: Accelerometer-based fall detection is experimental. It can miss very soft falls (e.g., sliding off a low sofa) or trigger on hard impacts (e.g., throwing the phone onto a desk). The 10-second cancel countdown, sensor debug screen, and fall simulation button exist specifically to accommodate these real-world constraints.
- **Vendor-Specific Lock Screen Behavior**: On certain OEM versions of Android (such as OxygenOS or MIUI), full-screen intents from background services over a locked display may be rendered by the OS as a high-priority heads-up notification rather than an instant display wake. Tapping the notification immediately opens the countdown.
- **Credential Security in Demo APK**: In this hackathon demo build, the Gemini API key and optional Twilio credentials are injected into `BuildConfig` via `local.properties`. While standard for local prototyping, this is **not safe for production distribution**. Never distribute a public APK built with personal API credentials.
- **Data Privacy & What Leaves the Device**:
  - No real health data, personal patient records, or account credentials are stored or tracked.
  - **What leaves the phone**:
    1. Medicine label and prescription camera images are transmitted over secure HTTPS to the Google Gemini API for inference.
    2. Alert SMS messages containing device GPS coordinates and the latest logged dose are sent over the cellular carrier network to the user's saved emergency contacts.
    3. If Twilio voice calling is enabled, the emergency alert text is sent via HTTPS to Twilio's REST API to generate text-to-speech audio.

---

## Future Work

1. **Secure Cloud Function Architecture**: Transition Twilio and Gemini API calls to an authenticated backend (e.g., Firebase Cloud Functions with Firebase App Check and Google Play Integrity) so zero API keys or secrets reside within the client APK.
2. **Regional Language & Audio Support**: Add multilingual UI translations and spoken audio prompts (Hindi, Spanish, Tamil, Marathi, etc.) for non-English speaking seniors.
3. **Dedicated Family Companion View**: A simple web or mobile dashboard allowing authorized family members to check dose compliance and battery status remotely without needing to receive an SMS.
4. **Smartwatch & Wearable Integration**: Companion Wear OS app to utilize wrist-worn accelerometers and heart rate sensors for dramatically higher fall detection accuracy and immediate haptic feedback.
5. **On-Device Vision Fallback**: Integrate on-device OCR (via Google ML Kit) to read printed medicine names locally when no internet connection is available.
6. **Machine-Learned Fall Detection Tuning**: Train a lightweight on-device classifier on benchmark elderly movement datasets to reduce false positives while capturing slow falls.
