# Nuvexa Drive

Nuvexa Drive 1.0.0 is an Android local-first file server and drive. The phone itself can host the primary file service with no external Nextcloud installation, server URL, account or login required. The repository still preserves the mature Nextcloud Android 35.0.0 client baseline for inherited file/media code and future optional interoperability.

## Nuvexa Local — self-hosted on the phone

The default launch path is now **Nuvexa Local**. Opening the app starts a native HTTP server inside the same APK and exposes Nuvexa's private storage to other devices on the same LAN through an authenticated local address.

Implemented local-server properties:

- embedded `ServerSocket` HTTP service; no external server app;
- foreground `connectedDevice` service for LAN availability;
- automatically selected port in the 8787–8797 range;
- random persistent pairing token with manual rotation;
- native import, folder creation and delete controls;
- browser portal for LAN upload, browse, download, folder creation and deletion;
- atomic upload staging before replacement;
- canonical-path enforcement against traversal outside Nuvexa storage;
- no internet/cloud dependency for primary local operation.

Files are currently stored in Nuvexa's app-private storage. Uninstalling the app can therefore remove this local data; durable user-selected SAF storage is a separate migration milestone and must not be implied as implemented.

See [LOCAL_SERVER.md](LOCAL_SERVER.md).

## Current baseline

This repository intentionally imports the complete functional Android baseline instead of rebuilding mature WebDAV/account/sharing behavior from scratch.

Current baseline includes file browsing, upload/download, folders, search, favorites, recent files, sharing, offline files, auto-upload, albums, Media3 playback, tags, E2EE support, multiple accounts, Android document/share integration, passcode/device credentials, WorkManager/background transfer infrastructure and the upstream locale catalog.

## Nuvexa 1.0 engineering priorities

1. Data integrity and reliable transfer recovery.
2. Simple, professional UI without button/card overload.
3. Automatic Android locale selection.
4. Light/dark/adaptive Android layouts.
5. Signed, reproducible release artifacts.
6. Continuous regression testing of known cloud-client failure modes.

## Reliability contract

Nuvexa must never silently lose, corrupt, endlessly retransmit or duplicate a file because of network loss, process death or restart. Every Nuvexa-specific reliability change must be covered by an appropriate regression test before it is described as complete.

Implemented Nuvexa-specific hardening currently includes an active-transfer Network Guard that re-checks Wi-Fi-only, charging-only and power-saver policy while an upload is actually transferring data, plus two-phase remote-size verification before destructive local upload behavior. Both have unit regression coverage.

See RELIABILITY.md and TESTING.md.

## Build and release integrity

The project currently targets Android API 36 and is built with Java 21. Release CI runs unit tests and lint before assembling the release artifact. Production artifacts use one stable Nuvexa signing identity; the private key is intentionally kept outside this public repository and the public certificate fingerprint is documented in SIGNING.md.

See BUILDING.md and SIGNING.md.

## Source and licensing

Nuvexa Drive is based on Nextcloud Android. Original copyright/SPDX notices and applicable GPL/AGPL obligations are retained. Nuvexa branding assets are separate brand assets and are not Nextcloud trademarks.

See LICENSE.txt, LICENSES/, REUSE.toml and THIRD_PARTY_NOTICES.md.

## Status

The repository is in the Nuvexa 1.0 hardening phase. Documentation distinguishes implemented inherited functionality from Nuvexa-specific work still requiring validation; a feature is not considered done merely because it compiles.
