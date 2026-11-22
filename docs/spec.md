# ElderPal Product Spec

## Purpose
A safety assistant for elderly people. Helps them take the right medicine at the right time, and alerts family when something goes wrong.

## Users
- Elder: weaker eyesight, low tech comfort. Needs big text, one action per screen, plain words.
- Family contact: receives alerts by SMS (and optionally a voice call later).

## Features

### 1. Medicine schedule and reminders
- User adds medicines manually: name, dose, times per day, with or without food. (Prescription photo import is optional and comes later.)
- Home shows the next medicine due. Schedule screen lists today's doses with status: upcoming, taken, missed.
- A notification fires at each dose time and opens the reminder screen: "Check medicine" or "Remind me in 10 minutes".
- Data is stored locally on the phone.

### 2. Medicine label check (main demo feature)
- User photographs the medicine label before taking it.
- The photo is sent to the Gemini API with the current schedule and current time.
- Gemini returns one of four results: OK (right medicine, due now), WRONG (not on the list), NOT_DUE (on the list but not due yet), UNSURE (label unreadable).
- The result screen matches the state. NOT_DUE allows "Take anyway" only after a second confirmation. OK allows "Mark as taken", which writes to the dose log.
- Never claim certainty. UNSURE and low confidence must say so and tell the user to ask a pharmacist or family member.

### 3. Emergency contacts
- User saves contacts: name, relationship, phone number. One contact can be marked "call first".
- The screen explains what contacts receive.

### 4. SOS
- Large red SOS button, always visible on Home.
- Press shows a confirmation (Send / Cancel). On Send: SMS to all contacts including the elder's location link and the recent medicine log, and a call to the "call first" contact (dialer fallback if permission denied).

### 5. Fall detection
- Uses the phone's motion sensors in the background.
- On a suspected fall: full-screen "Are you okay?" with a 10-second countdown. "I'm OK" cancels. "I need help now" or countdown end sends the same alert as SOS.

### 6. Alert message
- Format: "ElderPal alert: Fall detected at 4:12 PM. Last medicine taken: Blood Pressure pill at 3:45 PM. Location: <maps link>"
- Medicine log in the message is the creative hook of the project.

### 7. Permissions
- Camera, notifications, SMS, location, motion, phone call. Each has an explainer screen before the system prompt.

## Optional / later
- Prescription photo to schedule (scan-prescription screens)
- AI voice call to family via a cloud service (pending feasibility check)
- Spoken alert on the elder's own phone using Android's built-in text-to-speech

## Not in scope
- Auto-dialing emergency services
- Real patient data, accounts, or cloud storage of health data
- Medical advice or diagnosis

## Screen to design file map
- Home: home.png
- Schedule: schedule.png, schedule-empty.png
- Add medicine: add-medicine.png
- Scan prescription: scan-prescription.png, -loading, -review, -low-confidence
- Reminder: reminder.png
- Check medicine: check-medicine.png, check-medicine-loading.png
- Results: result-ok.png, result-wrong.png, result-not-due.png, result-not-due-confirm.png, result-unsure.png
- SOS: sos.png (no "sent" screen yet)
- Fall alert: fall-alert.png, fall-alert-sent.png
- Contacts: contacts.png, contacts-add.png, contacts-empty.png
- Alert preview: alert-preview.png
- Permissions: permissions.png, plus -location, -motion, -notifications, -sms