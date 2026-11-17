# RS Voice Setup — Cartesia (Free-First)

Last updated: 2026-11-17

## Confirmed decisions
- The voice recording belongs to Boss (RS RAKIB), who confirmed it may be used for RS voice cloning.
- Boss created a Cartesia account and reports that an Instant Voice Clone has been generated.
- Boss has a Cartesia API key. The key must never be sent in chat or committed to this repository.
- The supplied `ai.mp3` is a generated voice sample shared for review; do not assume it is the original recording.
- RS must use the same recognizable speaker identity for Bengali, English, and Hindi where Cartesia supports it; test each language before claiming parity.
- Free-first policy: no paid plan, subscription, or payment details without Boss's explicit approval. Free credits and feature access may change.

## Provider facts and caveats
- Cartesia's official language page lists Bengali and Hindi among supported languages; its current model/API documentation should be checked for the exact model and locale used at integration time.
- Cartesia's public materials describe short-sample Instant Voice Cloning and voice localization. Language availability can differ between clone creation, TTS models, and voice-localization features.
- Cartesia plan information has shown inconsistent statements about free Instant Voice Clone eligibility. Use only features actually enabled in Boss's logged-in account; do not upgrade or enter payment details.
- A successful Playground preview is not proof that the API can access the voice or that API usage is free. Verify API access and remaining free credits in the account.

## Credentials and privacy
- Never paste CARTESIA_API_KEY into ChatGPT, source files, README, Git commits, APKs, screenshots, or public issues.
- The Cartesia API key is a secret credential. Voice ID is an identifier, not an API key, but should still be shared only when needed.
- Do not upload Boss's original voice recording to another provider or service without his explicit approval.
- Do not commit voice recordings or generated audio samples to this public repository.
- Do not put a long-lived Cartesia API key in the Android app. APK contents can be extracted.
- The Android app must call a protected HTTPS backend. The backend stores CARTESIA_API_KEY as a server-side secret and calls Cartesia.
- Do not create a public unauthenticated TTS proxy. Add app authentication, request/rate limits, text length limits, safe error handling, and logging that never records secrets.

## Planned integration contract
1. Android sends response text and selected language to the RS backend over HTTPS.
2. Backend authenticates the app request and enforces rate/text limits.
3. Backend reads CARTESIA_API_KEY and RS_CARTESIA_VOICE_ID from private environment secrets.
4. Backend requests TTS from Cartesia using the verified voice and a currently supported model/API version.
5. Backend returns audio bytes in a format supported by Android playback.
6. Android plays the response and updates RS's face to SPEAKING; on completion it returns to the appropriate state.
7. If network/provider access fails or free credits are exhausted, RS falls back to Android TextToSpeech and clearly handles the fallback.

## Next steps for Boss
1. In Cartesia Voice Library, open the created RS voice and confirm its name and Voice ID. Do not share the API key.
2. In the Cartesia Playground, test the same voice with one short Bengali, English, and Hindi sentence. Confirm whether the voice is available for each language and whether the account asks for an upgrade.
3. Check Usage/Subscription to confirm remaining free credits and whether API access is enabled. Do not upgrade or add payment details.
4. Share only the Voice ID and the result of the three language tests in chat.
5. A protected free-tier backend still needs to be selected and deployed before the Android app can safely call Cartesia. GitHub Actions secrets alone are not a runtime backend.

## Current implementation status
- [x] Voice ownership and permission confirmed by Boss.
- [x] Cartesia account created (reported by Boss).
- [x] Instant Voice Clone generated (reported by Boss).
- [x] Generated sample audio shared by Boss.
- [x] Free-first policy documented.
- [x] Cartesia Voice ID received from Boss (kept out of this public repository).
- [x] Boss reports the same clone sounds perfect in Bengali, English, and Hindi.
- [ ] Account free API access and remaining credits verified.
- [ ] Protected backend selected, implemented, and deployed.
- [ ] Android remote-audio playback integrated.
- [ ] Real-device voice test completed.

## Confirmed voice
- Voice ID was supplied by Boss in chat. It is intentionally not stored in this public repository; configure it as a private Worker secret named `RS_CARTESIA_VOICE_ID`.
- Boss reports successful, satisfactory tests in Bengali, English, and Hindi. This is a user-reported result, not an independent device/API verification.

## Current Android fallback
The Android app currently uses the platform TextToSpeech engine. Cartesia is not yet connected. Keep the fallback until the protected backend and real-device tests pass.
