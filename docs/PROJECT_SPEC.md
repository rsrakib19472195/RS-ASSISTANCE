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
- Boss confirmed on 2026-11-17 that the supplied `RS ESPORT.mp3` is his own voice and may be used for voice cloning.
- Boss explicitly rejected ElevenLabs and paid voice services. Project requirement: build with free/open-source tools and avoid paid subscriptions or per-request APIs.
- Temporary voice output: Android's built-in TextToSpeech (free, device-provided; voice quality and offline availability depend on the installed engine/language pack).
- Research candidates for a later custom-voice prototype: `HayaiTTS` / sherpa-onnx for on-device TTS and supported ZipVoice/Pocket cloning paths; local Bengali cloning experiments such as Chatterbox-Bangla / XTTS-based projects. These are candidates only, not yet integrated or verified on Boss's phone. Check each model's license, Bengali quality, RAM/storage needs, and device performance before adoption.
- Prefer on-device/offline inference where practical. A free/open-source model may still require a capable PC for setup or model preparation; no promise of high-quality real-time cloning on the phone until tested.
- Emotion-aware delivery: vary tone, pace, and expression appropriately without faking feelings or making manipulative claims.
- Same cloned speaker identity across Bangla, English, and Hindi is a core requirement: RS must sound like Boss in all three languages, not switch to unrelated stock voices.
- First multilingual model candidate: Maya 2 Native, whose published language list includes Bengali, English, and Hindi and describes cross-lingual voice cloning. It is a candidate for proof-of-concept, not yet integrated or tested. Its published model size is about 2.5B parameters / ~5 GB BF16 weights, so assume a local PC/server is needed initially; do not promise direct Android inference until benchmarked.
- Chatterbox Multilingual is not the primary candidate because its published general model language list does not include Bengali.
- Prefer one local/offline inference service on a PC or self-hosted machine during development, with the Android app connecting over the user's own network; later investigate quantization/on-device deployment. No paid API key is required for this proof-of-concept.
- Never commit API keys or private credentials to the repository. Avoid paid credentials entirely unless Boss later changes the zero-cost requirement.

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
3. Free/open-source custom voice integration and expressive speech (no ElevenLabs).
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
- MainActivity includes the premium dark/emerald UI, animated AssistantFaceView, Bengali SpeechRecognizer input, Android TextToSpeech fallback, and in-app Sleep/Wake controls.
- The current reply logic is still rule-based; the AI brain is not connected.
- Sleep mode currently cancels in-app recognition and disables the talk button; it is not a background service or system-wide microphone shutdown guarantee.
- No ElevenLabs integration, custom voice, long-term memory, WhatsApp automation, notification reader, wake-word engine, or physical robot support is implemented yet.

## Next development sequence
### P0 — Project foundation (start here)
- Preserve the existing working app and build workflow.
- Add a durable specification/checkpoint document (this file).
- Add modular architecture plan before integrating external AI or voice services.
- Establish secure configuration/secrets handling.
- Rework UI in small, buildable steps.

### P1 — Premium RS interface
- Premium dark/emerald interface and animated face states have been implemented in the starter UI.
- Verify the UI and Sleep/Wake behavior on Boss's Android phone; refine based on actual device feedback.
- Keep the app buildable after each increment.

### P2 — AI conversation
- Add a protected backend/API integration; do not embed provider secrets in the APK.
- Add Bangla-first language handling with English/Hindi selection or detection.

### P3 — Free voice and modes
- Keep Android TextToSpeech as the initial zero-cost fallback.
- Evaluate an open-source local TTS/voice-cloning engine; first test Bengali output and model license on a development machine, then benchmark Android integration.
- Add Boss's own voice reference only to the selected authorized model; do not upload it to a third-party hosted service without a new explicit decision.
- Add Active/Sleep/Off controls and Android-compliant foreground service behavior.

### P4 — Memory and Android integrations
- Add user-controlled persistent memory.
- Add permission-gated notifications and Android actions.
- Add WhatsApp draft/confirmation/send workflow where supported.

## Inputs needed from Boss (do not block initial UI/spec work)
- Voice ownership confirmed: `RS ESPORT.mp3` is Boss's own voice. No further ownership confirmation needed for this file.
- ElevenLabs is not part of the plan. Do not request an ElevenLabs Voice ID or API key.
- No API keys are needed for the initial local voice proof-of-concept. For the AI brain, first assess local open models and hardware; only ask Boss for a provider key if a later optional hosted provider is chosen.
- Test APKs on the target Android phone and report actual results.


## Progress log
### 2026-11-17 — Initial specification and first UI increment
- Added this durable product specification/checkpoint and linked it from README.
- Added `AssistantFaceView.java`: a custom-drawn animated RS avatar with idle, listening, speaking, happy, thinking, concerned, surprised, and sleeping expression states.
- Reworked `MainActivity.java` into a dark premium green RS interface with Boss greeting, voice-input button, expression updates, and an explicit Sleep/Wake UI control.
- Sleep mode in this first UI increment cancels current speech recognition, stops TTS, and disables the in-app talk button. It is not yet a background service and has not been device-tested.
- The existing Android SpeechRecognizer and device TTS remain as temporary prototype components; ElevenLabs and the AI brain are not connected.
- GitHub Actions build for commit `9434c0a87986121c67696b2629813615c2444d2b` is running; do not mark the build successful until GitHub reports success.
- Next checkpoint: verify the build, install on Boss's Android phone, inspect the new face/UI and Sleep/Wake behavior, then proceed with modular AI/backend and voice integration.


### 2026-11-17 — Voice decision and build verification
- Boss confirmed `RS ESPORT.mp3` is his own voice.
- Boss chose a zero-paid-services policy for the full RS project and explicitly declined ElevenLabs.
- Voice plan changed: use Android built-in TTS as the immediate free fallback; research and prototype open-source on-device/local voice cloning for Boss's own voice.
- Initial research candidates: HayaiTTS/sherpa-onnx (Android offline TTS with selected cloning-capable model families), and Bengali-capable local pipelines such as Chatterbox-Bangla / XTTS-based projects. None is integrated or device-verified yet; licenses, quality, memory use, and Motorola compatibility must be checked.
- GitHub Actions Build #11 (run `44041333269`, UI/avatar code) completed successfully, including APK upload.
- GitHub Actions Build #12 (run `44041397239`, checkpoint/docs update) completed successfully, including APK upload.
- Device installation and real-phone testing remain pending.
- Next step: build a small free voice-engine proof of concept, starting with Bangla quality and hardware benchmarking before integrating it into the RS app.


### 2026-11-17 — Voice ownership and setup checkpoint
- Boss confirmed the supplied voice recording is his own voice.
- Boss has not created an ElevenLabs account yet.
- Added `docs/VOICE_SETUP.md` with account setup steps, private voice-sample handling, secure credential rules, the planned Android-to-backend-to-ElevenLabs flow, fallback behavior, and outstanding tasks.
- Voice cloning and ElevenLabs API integration are not implemented yet; waiting for the account, Voice ID, and a protected backend decision.
- GitHub Actions build #11 (UI/avatar code) and build #12 (documentation checkpoint) both completed successfully, including APK build and artifact upload.
- Repository write access was verified; default branch is `main`.
- Next step for Boss: create the ElevenLabs account and create the voice privately. Share only the Voice ID later; never share the API key in chat or commit it.
- Next development step: after the account/Voice ID is available, implement the protected backend and Android audio playback integration, retaining device TTS as a fallback.


### 2026-11-17 — Multilingual same-voice requirement
- Boss confirmed RS must speak Bangla, English, and Hindi in the same cloned voice identity.
- Researched candidate Maya 2 Native: published model card lists Bengali (`bn`), English (`en`), Hindi (`hi`), zero-shot/cross-lingual cloning, Apache-2.0, and about 2.5B parameters / 5 GB BF16 weights. This is promising but not yet tested; its size suggests initial inference on a PC/self-hosted machine rather than directly on the Android handset.
- Chatterbox Multilingual was not selected because its published general multilingual language list omits Bengali.
- Initial architecture direction: Android RS app + local/self-hosted inference service for voice and later AI, with no paid API dependency. Do not expose a public unauthenticated server; protect any LAN endpoint and keep the voice reference local/private.
- No API key or payment is required from Boss now. Needed later: the actual `RS ESPORT.mp3` file accessible in the active project workspace (re-upload only if unavailable), plus access to a PC/laptop for model setup/testing if the Android phone cannot run the model efficiently. Boss's phone model is Motorola G57 Power; verify exact RAM/storage and Android compatibility before deployment.
- Next: inspect available project audio asset, build a minimal multilingual voice proof-of-concept on a suitable local machine, test Bangla/English/Hindi with the same reference voice, then design Android integration. No model is marked complete until audio quality and performance are tested.


### 2026-11-17 — Cartesia evaluation (zero-cost constraint)
- Boss requested Cartesia voice cloning at https://www.cartesia.ai/product/voice-cloning as the preferred hosted voice candidate.
- Official Cartesia voice-cloning page states the public demo can be tried free, but recordings uploaded to that demo are used for the demo and then deleted; a reusable Instant Voice Clone in the account/API is available on Pro and above. The official pricing page currently lists Free at $0 with 20K monthly credits for TTS/STT, and Pro at $5/month with Instant Voice Cloning. Therefore, Cartesia is not a fully free persistent voice-cloning/API solution under the current published plan.
- Decision: use Cartesia's free public demo only for an optional quality test; do not subscribe, do not assume a demo clone can be retained or called by API, and do not upload Boss's voice sample without Boss's explicit approval for that upload.
- If Boss proceeds with a hosted demo test, use his own voice only, keep the recording clean (single speaker, quiet room, no music), and test Bangla, English, and Hindi separately. The website supports voice cloning and says its TTS supports 44 languages; actual cross-lingual similarity and Bengali pronunciation must be verified with real samples.
- Keep the zero-cost requirement: if Cartesia's reusable clone/API requires payment, continue researching free/open-source/local alternatives. Do not request a paid API key or payment details.
- No Cartesia API integration has been implemented; no Cartesia credentials or voice ID are available. Do not claim this voice feature is complete.


### 2026-11-17 — Zero-cost-first policy reaffirmed; Cartesia pricing discrepancy
- Boss reaffirmed: make the project usable at $0 now; consider paid services only later when he has money and explicitly chooses them. Prioritize fast, small, testable Android milestones rather than promising the entire advanced assistant immediately.
- Rechecked Cartesia's official current pages. The Sonic FAQ says Free ($0, 20K credits/month) includes access to Instant Voice Cloning, but the same official pricing page's plan comparison lists Instant Voice Cloning under Pro ($5/month), not Free. This is an inconsistency in Cartesia's own public information. Do not promise a persistent/API-usable clone on Free until the actual account UI confirms it. Free plan currently lists 20K credits/month for TTS/STT; overages are unavailable on Free. Avoid any payment method or paid upgrade.
- Cartesia may be used only if the user's account actually exposes the required cloning feature at $0 and the user accepts uploading the voice sample. Otherwise use Android built-in TTS immediately and evaluate open-source on-device engines (e.g. HayaiTTS/sherpa-onnx) for offline speech; model-by-model language support and voice cloning must be tested. No unverified claim that these engines clone Boss's voice in Bengali, English, and Hindi.
- Development priority: (1) keep app buildable and improve existing Android foundation, (2) working free voice input/output, (3) core AI chat with a genuinely free/local option or clearly disclosed free-tier limits, (4) permissions-gated phone actions, (5) memory and notification integrations. Test each milestone on Boss's Android phone.
- No paid APIs, subscriptions, or payment details without Boss's later explicit approval.


## 2026-11-17 — Latest voice milestone (supersedes earlier Cartesia research notes above)
- Boss has created a Cartesia account, reports a reusable Instant Voice Clone, and reports satisfactory Bangla/English/Hindi voice tests. This is based on Boss's account and listening tests; account entitlement and remaining free credits have not been independently verified.
- Protected Cloudflare Worker deployed at `https://rs-assistance-api.tahmidjod19.workers.dev/`. Boss tested it via Hoppscotch: authenticated POST generated and played the cloned voice; GET correctly returned `POST required`.
- Worker secrets are configured privately by Boss: `CARTESIA_API_KEY`, `RS_CARTESIA_VOICE_ID`, `RS_APP_TOKEN`. No secret values or voice ID belong in this repository.
- Android app now has a Cartesia WAV playback client, calls the Worker for Bengali replies, and retains Android TextToSpeech as fallback. INTERNET permission added.
- GitHub Actions reads repository secret `RS_APP_TOKEN` and passes it as a build property. Boss must add the same token privately under repository Settings → Secrets and variables → Actions → New repository secret (name `RS_APP_TOKEN`) before producing a functional test APK. If the original token was not retained, rotate it by setting a new random token in Cloudflare Worker secrets and GitHub Actions; do not send it in chat.
- Prototype limitation: any token embedded in an APK can be extracted. This is not strong production authentication; add stronger device/session authentication and rate limiting before wider release.
- Status: code committed; build and real-device integration are NOT yet verified. Cartesia free credits/entitlement remain subject to account limits. No paid upgrade or payment details permitted without Boss's explicit later approval.

## 2026-11-17 — Android permission, assistant, and action integration update
- First app launch now requests Android runtime permissions for microphone, camera/flashlight, contacts (WhatsApp recipient lookup), and notifications on Android 13+.
- Android RoleManager request flow offers RS as the default assistant. Android still requires Boss to approve the system role picker; the app cannot silently assign itself.
- Added a NotificationListenerService. After Boss enables Notification Access in Android Settings, RS stores notification previews locally. WhatsApp read-aloud is opt-in and off by default.
- Added a WhatsApp draft flow for supported voice patterns: resolve a contact, show recipient/message confirmation, open the WhatsApp chat with the draft prefilled. RS does not press Send; Boss must review and send.
- Added brightness up/down actions. Android requires the separate Modify System Settings approval before RS can change brightness.
- Added an opt-in foreground microphone service for the “RS” wake phrase. It displays a persistent notification and stops when Sleep Mode is selected. Current prototype uses Android SpeechRecognizer in a restart loop; it is not a dedicated offline wake-word model, may depend on the device speech-recognition provider/network, and requires real-device privacy/battery testing.
- Existing safe actions include flashlight, media volume, battery status, opening apps, Settings/Wi-Fi/Bluetooth Settings, and opening the Clock alarm setup UI.
- Local conversation history is stored on-device and can be cleared from the app.
- GitHub Actions Build #71 (run `44074511499`, commit `62d863c9827d84a94879582df92658f4ef3adeec`) completed successfully and uploaded `RS-ASSISTANCE-debug-apk`.
- Important: successful compilation is not proof of real-device behavior. Default assistant selection, runtime/special permissions, speech recognition availability, WhatsApp notification parsing, wake phrase reliability, and battery use still require testing on Boss's Motorola phone.
- Not yet implemented: true offline wake-word detection, accessibility-based cross-app UI control, robust natural-language action planning from the AI Brain, reminders/contacts/calls beyond the WhatsApp draft flow, and full end-to-end real-device verification. Do not describe these as complete.
