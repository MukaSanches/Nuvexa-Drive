# Nuvexa Test Matrix

Release candidates must cover, as infrastructure permits:
- Samsung / One UI, Xiaomi/Redmi/POCO, Motorola and Pixel.
- Wi-Fi to mobile-data handoff and Wi-Fi-only enforcement.
- Airplane mode and unstable networks.
- App/process death and device reboot during transfer.
- Server offline/recovery and response loss after upload commit.
- Zero-byte and duplicate-name conflict scenarios.
- Large uploads/downloads including resumable cases.
- Full local/server storage.
- Reinstall/re-login reconciliation.
- Remote-only file open, media playback and share sheet.
- Light/dark mode, long translated strings and RTL.
- Release APK installation and smoke test, not only debug builds.

A failing critical test blocks an approved release.
