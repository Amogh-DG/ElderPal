# ElderPal Agent Instructions

Standing rules for the AI coding assistant. Read this file at the start of every task and follow it.

## 1. Project facts
- App name: ElderPal. A safety assistant for elderly people (medicine reminders, photo check of medicine labels using the Gemini API, fall detection, SOS button).
- The project folder is named "evo". The app and project name is "ElderPal". Never rename the folder.
- Native Android, Kotlin, Jetpack Compose. Minimum SDK is API 26.
- Never change the package name. Never recreate the project.
- The developer is a beginner working solo on a hackathon, so keep everything simple.

## 2. Working style
- Do only the task given in the prompt. Nothing more.
- Do not add extra features, screens, or UI elements that were not requested.
- Do not add new dependencies unless the task needs them. If you add one, tell me its name, version, and why.
- Do not rewrite or reformat files unrelated to the task.
- If a task is too big or unclear, stop and ask instead of guessing.

## 3. Honesty
- Use only standard approaches from the official Android and Kotlin documentation.
- Never invent libraries, APIs, settings, menu names, or file names.
- If you are not sure about something, say "I'm not sure" instead of guessing.
- If something fails or cannot be done, say so plainly. Do not hide it or fake it.

## 4. Secrets and privacy
- API keys (such as the Gemini key) live only in local.properties and are read through BuildConfig.
- Never put keys, passwords, phone numbers, or personal data in code, logs, comments, docs, or commits.
- local.properties must stay in .gitignore. A local.properties.example file with fake placeholder values stays in the repo.
- Use only fake sample data (fake prescriptions, fake contacts). No real health data.
- Any future cloud service credentials (for example Twilio) must never be inside the app or the repo.

## 5. Design
- A design reference exists in /design. Before writing any UI code, read design/design.md and look at the screen images that match the task.
- Follow the layout, colors, fonts, spacing, and button sizes shown.
- Do not add UI elements that are not in the design.
- Hard accessibility rules: body text 18sp or larger, headings 28sp or larger, main buttons at least 64dp tall, high contrast, and color is never the only signal (always add an icon or words).
- The handwritten font is for the greeting only. All functional text uses the plain sans-serif.
- Only use fonts and assets with open licenses, and tell me where each one came from.
- If something in the design cannot be done in standard Jetpack Compose, or the design is unclear, tell me instead of guessing.

## 6. Safety rules
- ElderPal is an assistive helper, not medical advice. Do not write text that claims otherwise.
- The medicine label check must never claim certainty. When the label is unclear, it must say so and tell the user to ask a pharmacist or family member.
- Never auto-dial emergency services (such as 112 or 911). Calls and texts go to family contacts only.
- Fall and SOS alerts must include a cancel step before sending, to avoid false alarms.
- When testing calls or SMS, use only non-emergency numbers that belong to me.

## 7. Code style
- Simple, readable code. Prefer clear names over clever tricks.
- Put a short plain-language comment at the top of each file explaining what it is for.
- Keep files small and focused. Use the packages: screens, data, and ai.
- Handle errors gracefully (no crashes on a missing permission, no network, or a bad API response).

## 8. End of every task
When finished, always provide:
1. A list of every file changed or created, with one sentence on what each does.
2. How to run and test it on my physical Android phone.
3. A simple plain-language explanation of how the code works, so I can explain it in a demo video.
4. Anything in the design you could not match exactly.
5. Anything you were unsure about.

Then stop and wait for the next task.