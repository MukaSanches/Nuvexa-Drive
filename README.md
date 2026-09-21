# Nuvexa Drive

Nuvexa Drive 1.0.0 is an Android cloud, files, photos, media, backup and sync client built from the stable Nextcloud Android 35.0.0 codebase and evolved as an independent product.

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

Implemented Nuvexa-specific hardening currently includes an active-transfer Network Guard that re-checks Wi-Fi-only, charging-only and power-saver policy while an upload is actually transferring data, with unit regression coverage.

See RELIABILITY.md and TESTING.md.

## Build and release integrity

The project currently targets Android API 36 and is built with Java 21. Release CI runs unit tests and lint before assembling the release artifact. Production artifacts use one stable Nuvexa signing identity; the private key is intentionally kept outside this public repository and the public certificate fingerprint is documented in SIGNING.md.

See BUILDING.md and SIGNING.md.

## Source and licensing

Nuvexa Drive is based on Nextcloud Android. Original copyright/SPDX notices and applicable GPL/AGPL obligations are retained. Nuvexa branding assets are separate brand assets and are not Nextcloud trademarks.

See LICENSE.txt, LICENSES/, REUSE.toml and THIRD_PARTY_NOTICES.md.

## Status

The repository is in the Nuvexa 1.0 hardening phase. Documentation distinguishes implemented inherited functionality from Nuvexa-specific work still requiring validation; a feature is not considered done merely because it compiles.
