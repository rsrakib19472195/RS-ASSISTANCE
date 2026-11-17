# RS ASSISTANCE 🤖

**RS — Your Personal AI Companion**

An Android-first personal assistant project owned and developed by RS RAKIB. RS addresses the owner as **Boss** and is planned to support Bangla first, with English and Hindi.

## Current starter features
- Java Android application
- Premium dark/emerald interface with animated RS face and emotion states
- Bengali speech input using Android SpeechRecognizer
- Spoken responses using Android TextToSpeech as a temporary fallback
- In-app Sleep/Wake controls (not yet a background service)
- Microphone permission requested when the user taps the talk button
- GitHub Actions workflow for debug APK builds

## Development status
This repository currently contains a runnable starter, not the complete planned assistant. The AI brain, ElevenLabs voice integration, persistent memory, WhatsApp workflows, notification reader, compliant background service, camera vision, and smart-home/robot integrations are not yet implemented. The animated face and initial Sleep/Wake UI are implemented but still need real-device testing.

## Project specification and checkpoint
See [docs/PROJECT_SPEC.md](docs/PROJECT_SPEC.md) for the full product vision, privacy and safety requirements, current verified status, and roadmap. See [docs/VOICE_SETUP.md](docs/VOICE_SETUP.md) for the ElevenLabs setup and secure voice-integration plan.

## Security
Never commit API keys, voice-provider credentials, or private tokens. Configure secrets through GitHub Actions Secrets or a protected backend.
