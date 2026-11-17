# RS Voice Setup — ElevenLabs

Last updated: 2026-11-17

## Confirmed decisions
- The voice recording belongs to Boss (RS RAKIB).
- Boss has not created an ElevenLabs account yet.
- RS should use a natural Bangla-first voice, with English and Hindi where the selected voice/model supports them.
- The current Android app still uses Android TextToSpeech as a temporary fallback. ElevenLabs is not connected.

## Setup sequence

### 1. Create an ElevenLabs account
1. Open https://elevenlabs.io/ in a browser.
2. Create an account using an email address controlled by Boss.
3. Verify the email and sign in.
4. Review the current plan, voice-cloning eligibility, privacy terms, and available languages before uploading audio. Features and limits can vary by plan and region.

### 2. Prepare the voice sample
- Use only the recording that Boss owns and is comfortable uploading.
- Prefer a clean recording with one speaker, little background noise, no music, and natural speech.
- Do not upload another person's voice or any recording without permission.
- Keep the original file private. Do not commit it to this public GitHub repository or share it in a public issue.
- If the supplied RS ESPORT.mp3 is the intended sample, Boss can upload it directly in the provider's private voice-creation flow after reviewing the provider's requirements.

### 3. Create the voice
1. In ElevenLabs, open the voice creation / voice cloning area available to the account.
2. Follow the provider's consent and verification steps.
3. Add the recording and create the voice.
4. Save the resulting Voice ID privately. The Voice ID is an identifier, not the API key.

### 4. Protect credentials
- Never paste the ElevenLabs API key into ChatGPT, a source file, README, Git commit, APK, or screenshot.
- The API key must stay on a protected backend/server as an environment secret.
- The Android app should call the RS backend; the backend calls ElevenLabs and returns generated audio.
- Do not call ElevenLabs directly from the Android APK with a long-lived secret, because APK contents can be extracted.
- Do not create a public unauthenticated text-to-speech proxy. Before deployment, add app-to-backend authentication, request limits, input limits, and safe logging that never records secrets.

## Planned integration contract
1. Android sends the text, requested language, and allowed emotion/style to the RS backend over HTTPS.
2. Backend validates the app session and request limits.
3. Backend reads ELEVENLABS_API_KEY and ELEVENLABS_VOICE_ID from its private environment.
4. Backend requests speech from ElevenLabs.
5. Backend returns audio bytes and safe response metadata.
6. Android plays the audio and updates RS's face to SPEAKING; when playback ends, it returns to the appropriate state.
7. If the network/provider is unavailable, RS falls back to Android TextToSpeech and clearly indicates the fallback when useful.

## Information still needed from Boss
- Create the ElevenLabs account.
- Create the voice and provide the Voice ID (safe to share as an identifier, but do not share the API key).
- Later, choose and configure a protected backend host. The API key must be entered directly into that host's secret/environment settings.
- Test generated Bangla speech on the target Android phone for pronunciation, latency, and reliability.

## Current implementation status
- [x] Voice ownership confirmed by Boss.
- [x] ElevenLabs account status confirmed: not created yet.
- [x] Secure integration plan documented.
- [ ] ElevenLabs account created.
- [ ] Voice created and Voice ID available.
- [ ] Protected backend implemented and deployed.
- [ ] Android remote-audio playback integrated.
- [ ] Real-device voice test completed.