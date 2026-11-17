# RS ASSISTANCE — Product Specification & Development Checkpoint

Last updated: 2026-11-17

## Product identity
- Product/app name: RS
- Project/repository: RS ASSISTANCE
- Owner and developer: RS RAKIB
- Assistant calls the owner: Boss
- Default language: Bangla
- Additional languages: English and Hindi
- Product goal: an advanced, human-like personal AI companion on Android first, with a future path to a physical robot.

## Personality and identity
- RS must know its own name, role, capabilities, limitations, and developer/owner identity.
- RS should recognize the owner as Boss (RS RAKIB) using an appropriate, privacy-conscious authentication/recognition design. Do not claim reliable identity recognition from voice alone until implemented and tested.
- Warm, respectful, natural, emotionally expressive personality.
- Context-aware greetings, salam, welcome, and farewell; avoid repetitive or intrusive greetings.
- Praise constructive actions and respectfully warn Boss when a proposed action appears harmful, unsafe, or counterproductive. Explain why; do not pretend to be infallible.
- Natural conversation rather than requiring memorized command phrases.
- Ask for confirmation before consequential actions such as sending messages, deleting data, making purchases, or changing security settings.

## Voice and emotion
- User wants a distinct human-like voice using ElevenLabs, with the supplied audio file as a voice reference.
- Use only a voice the user owns or is authorized to clone/use. Confirm ownership/permission before cloning.
- Emotion-aware delivery: vary tone, pace, and expression appropriately without faking feelings or making manipulative claims.
- Support Bangla first, plus English and Hindi where the chosen voice/model supports them.
- Never commit API keys or private credentials to the repository. Store secrets in GitHub Actions Secrets or a protected backend.

## Face and interface
- Premium, unique, attractive animated assistant face/avatar.
- Expression states can include idle, listening, speaking, happy, concerned, thinking, surprised, and apologetic.
- Lip-sync or speaking animation should track generated audio where technically feasible.
- High-quality, accessible Android UI; prioritize responsiveness and battery efficiency.

## Assistant modes and privacy
- Active mode: user-enabled assistant features and permitted background service.
- Sleep mode: stop microphone capture and background listening; do not respond until explicitly awakened through an app control or another permitted wake mechanism.
- Off mode: stop assistant services and microphone use.
- Always show clear microphone/service status and provide an obvious stop control.
- Android background microphone operation must comply with OS foreground-service and permission rules. Do not promise invisible or unrestricted always-on listening.
- Notification, camera, accessibility, screen context, and contacts access each require clear explanation and explicit user permission.
- Do not silently inspect other apps or private content.

## Capabilities roadmap
1. Android app foundation and polished UI.
2. AI brain: natural-language understanding, online AI provider integration, graceful offline fallback.
3. ElevenLabs voice integration and expressive speech.
4. Personal memory: user profile, preferences, projects, people introduced by Boss, and user-controlled forget/delete flow.
5. Android actions: open apps, flashlight, volume, brightness, alarms/reminders, battery status, and supported settings.
6. WhatsApp workflow: identify recipient, draft message, read it back, ask Boss to confirm, then send using an Android-supported and policy-compliant method where available.
7. Notification reader: with Notification Access permission, identify app/sender and read message content aloud according to user settings.
8. Camera/vision and contextual assistance, only while explicitly enabled.
9. Emotional companion and music integration through supported apps/services.
10. Diagnostics, safe updates, smart-home integration.
11. Future physical robot hardware integration (not part of the current Android milestone).

## Safety and reliability
- Ask before external side effects, especially sending messages.
- Never send to an ambiguous recipient; ask Boss to choose.
- Do not perform financial transfers or destructive actions without strong confirmation and appropriate safeguards.
- Be transparent when offline, when a provider is unavailable, or when a requested Android action is unsupported.
- Keep API keys out of source code and build artifacts.
- Add tests and verify GitHub Actions builds after code changes.
- Do not claim a feature is complete until implemented and tested on a real device.

## Current repository baseline (verified 2026-11-17)
Repository: https://github.com/rsrakib19472195/RS-ASSISTANCE
Default branch: main
- Java Android starter app exists.
- Bengali SpeechRecognizer input exists.
- Android TextToSpeech output exists.
- Microphone permission is requested after the user taps the talk button.
- Android 11+ speech recognition service query is declared.
- Speech recognition errors are mapped to Bengali messages.
- GitHub Actions debug APK workflow exists.
- MainActivity currently replies only to a simple hello/হ্যালো greeting; the AI brain is not connected.
- Current UI is a basic native layout, not the planned premium animated face.
- No ElevenLabs integration, custom voice, long-term memory, WhatsApp automation, notification reader, wake-word engine, or physical robot support is implemented yet.

## Next development sequence
### P0 — Project foundation (start here)
- Preserve the existing working app and build workflow.
- Add a durable specification/checkpoint document (this file).
- Add modular architecture plan before integrating external AI or voice services.
- Establish secure configuration/secrets handling.
- Rework UI in small, buildable steps.

### P1 — Premium RS interface
- Create the app's visual design system and assistant face states.
- Keep the app buildable after each increment.

### P2 — AI conversation
- Add a protected backend/API integration; do not embed provider secrets in the APK.
- Add Bangla-first language handling with English/Hindi selection or detection.

### P3 — Voice and modes
- Integrate authorized ElevenLabs voice through a protected service.
- Add Active/Sleep/Off controls and Android-compliant foreground service behavior.

### P4 — Memory and Android integrations
- Add user-controlled persistent memory.
- Add permission-gated notifications and Android actions.
- Add WhatsApp draft/confirmation/send workflow where supported.

## Inputs needed from Boss (do not block initial UI/spec work)
- Confirm that the supplied voice recording is Boss's own voice or that Boss has permission to use it for voice cloning. If not, use a licensed voice.
- Later provide an ElevenLabs Voice ID and configure the API key privately in GitHub Secrets or a backend secret store. Never paste the API key into chat or commit it.
- Later choose an AI provider/model and supply its credentials privately if needed.
- Test APKs on the target Android phone and report actual results.
