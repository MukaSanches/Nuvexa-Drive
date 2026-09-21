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

See RELIABILITY.md and TESTING.md.

## Build

The project currently targets Android API 36 and is built with Java 21. See BUILDING.md.

## Source and licensing

Nuvexa Drive is based on Nextcloud Android. Original copyright/SPDX notices and applicable GPL/AGPL obligations are retained. Nuvexa branding assets are separate brand assets and are not Nextcloud trademarks.

See LICENSE.txt, LICENSES/, REUSE.toml and THIRD_PARTY_NOTICES.md.

## Reliability hardening in 1.0

The Nuvexa hardening branch adds explicit data-integrity gates on top of the inherited transfer engine:

- post-upload remote size verification before destructive local actions;
- protection against a non-empty source being accepted as a zero-byte/truncated remote copy;
- temporary-download size verification before promotion to the final destination;
- Wi-Fi-only policy re-evaluated while an upload is active;
- regression tests for upload verification, download promotion and network-policy transitions;
- independent Nuvexa QA/Dev provider identities and removal of upstream Firebase credentials from the Play flavor.

These changes remain subject to CI, device/server regression testing and release validation before 1.0 is marked approved.

## Status

The repository is in the Nuvexa 1.0 hardening phase. Documentation distinguishes implemented inherited functionality from Nuvexa-specific work still requiring validation; a feature is not considered done merely because it compiles.
