# ElderPal Design Spec

Android safety assistant for elderly people. Frame: 360 x 800 dp. Calm, warm, trustworthy.

## Color palette

- Primary teal: #0E5A6E. Main buttons, links, selected states, icons. White text on it is 7.8:1.
- Primary tint: #E1EFF2. Soft fills for cards, chips, "upcoming" status.
- Background: #FBF6EE. Warm cream, every screen.
- Surface: #FFFFFF. Cards, inputs, secondary buttons.
- Text: #26221D. Body and headings (14:1 on background).
- Text secondary: #58504A. Hints and dates (7.4:1 on background).
- Border: #E4D9C6. Card outlines. Input border: #8C8174 (3.8:1).
- Success green: #1B6B3A on tint #E3F2E7. Taken, right medicine, alert sent.
- Warning amber: text #8A5200, fill #F2A900, tint #FFF0CC. Not due yet, please check, fall countdown.
- Danger red: #B3261E on tint #FCE8E6, shadow #7F1712. SOS, wrong medicine, missed, send alert.
- Neutral grey: #4F5862 on tint #ECEEF0. Unsure result.
- Camera dark: #1B1814. Camera screens only.

Every status uses color plus an icon plus a word.

## Typography

Font: Atkinson Hyperlegible (Google Fonts, 400 and 700), made for low vision. Fallback: system-ui, Roboto, sans-serif.

- Display: 32 sp, bold, line height 40. Screen titles.
- Big: 40 to 44 sp, bold, line height 48 to 52. Times, reminder and fall headlines.
- Heading 2: 28 sp, bold, line height 36. Medicine name on home, empty states.
- Title: 24 sp, bold, line height 32. Card titles, buttons.
- Body: 20 sp, regular, line height 30. Minimum for reading text. Key messages use 22 / 32.
- Label: 22 sp, bold, line height 28. Field labels, link buttons.
- Small: 18 sp, regular, line height 26. Footer and captions. Nothing is smaller.

## Buttons, shape, spacing, icons

- Standard button: 72 dp tall, full width, 20 dp radius, 3 dp border, 24 sp bold label.
- Large button: 88 dp. Extra large (Send alert, I'm OK): 112 to 120 dp.
- Minimum touch target anywhere: 64 dp (back button, SOS chip, link buttons).
- Primary: teal fill, white text. Secondary: white or tint fill, teal border.
- Success: green fill. Danger: red fill with dark bottom shadow. Warning option: white with amber border.
- SOS bar (home): 112 dp, red, white circle badge. SOS chip (inner screens): 64 x 112 dp, top right.
- Cards: 24 dp radius, 16 dp padding, 2 dp border. Inputs: 72 dp tall, 16 dp radius, 3 dp border, 24 sp text. Pills: 36 dp tall, full radius.
- Spacing scale: 4, 8, 12, 16, 24, 32, 48. Screen margin 16 dp, gap between blocks 16 dp, gap between buttons 12 dp.
- Icons: outline style, 24 x 24 grid, 2.25 stroke, round caps and joins. Sizes 24, 32, 48, 64, 80 dp.

## Screens

Name in the canvas, then purpose.

- home: See the next medicine and reach every main action; SOS always visible.
- schedule: Today's doses with time, name, dose and status.
- schedule-empty: Empty state when no medicine is set up.
- add-medicine: Type a medicine by hand and save.
- scan-prescription: Camera with a framing guide to photograph a prescription.
- scan-prescription-loading: "Reading your prescription" wait state.
- scan-prescription-review: Confirm the medicines found; each is editable.
- scan-prescription-low-confidence: One unreadable part flagged for checking.
- reminder: Full-screen "Time for your pill" with Check medicine and a 10-minute snooze.
- check-medicine: Camera to show the medicine label.
- check-medicine-loading: "Checking your medicine" wait state.
- result-ok: Green. Right medicine, due now. Mark as taken.
- result-wrong: Red. Not on the list. Ask a pharmacist or family.
- result-not-due: Amber. Not due yet. Wait, or Take anyway.
- result-not-due-confirm: Second confirmation before taking early.
- result-unsure: Grey. Label unclear. Try again or ask for help.
- sos: Confirm sending an emergency alert, or cancel.
- sos-sent: Alert sent, with who was told.
- fall-alert: "Are you okay?" with a 10-second countdown.
- fall-alert-sent: Alert sent to family after the countdown ends.
- alert-preview: Example of the text message family receives.
- contacts: List of emergency contacts and what they receive.
- contacts-add: Form to add a contact.
- contacts-empty: Empty state for contacts.
- permissions: Camera. permissions-notifications: Reminders. permissions-sms: Text messages. permissions-location: Location. permissions-motion: Motion sensors. One sentence and one Allow button each, with a quiet "Not now".

The canvas file for home is named Main; its board title is "home".

## Accessibility

- Text is 18 sp or larger. Headings 28 sp or larger. All text passes WCAG AA (most pass AAA at 7:1).
- Color is never alone: every status has an icon and a word (Taken, Missed, Upcoming, Please check).
- Main buttons are 72 dp or taller; nothing tappable is under 64 dp.
- One main action per screen. SOS stays visible on home and as a chip on inner screens.
- Plain words, no medical jargon. Cancel and back are always available.
- Real links, buttons, labels and inputs, so TalkBack can read them. Icon-only controls have labels.
- Spinner animation stops when the phone's "reduce motion" setting is on.
- Every result screen ends with: "ElderPal is a helper, not medical advice."