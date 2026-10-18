# Female Voice Studio AI — Android APK project

This project packages the Java Android app as an installable debug APK using GitHub Actions. The free mode uses voices installed in Android's Text-to-Speech engine. Premium speech and voice cloning require your own compatible backend and provider credentials; they are not ready just by installing the APK.

## Build the APK on GitHub (no computer required)

1. Download and extract this ZIP on your phone.
2. Open https://github.com/new and create a repository named `FemaleVoiceStudioAndroid` (public or private).
3. Upload the **contents** of the extracted folder, not the outer folder itself. Ensure `app`, `.github`, `build.gradle`, `settings.gradle`, and `gradle.properties` appear at the repository root.
4. Open the repository's **Actions** tab. If asked, enable workflows.
5. Select **Build Android APK** and press **Run workflow**. If a push already triggered a build, open its latest run instead.
6. Wait for the run to finish successfully. Open the run, scroll to **Artifacts**, and download `FemaleVoiceStudio-debug-apk`.
7. Extract the downloaded artifact ZIP. Tap `app-debug.apk` to install it. Android may ask you to allow installation from that source.

## Notes
- The workflow builds a debug APK for testing, not a Play Store release.
- The free mode is local Android text-to-speech. Female voices and languages depend on the TTS engine and voice packs installed on the phone.
- Premium mode requires a separately deployed HTTPS backend implementing `/api/voices`, `/api/tts`, and `/api/clone`. Do not enter provider secrets into an untrusted app or share them publicly.
