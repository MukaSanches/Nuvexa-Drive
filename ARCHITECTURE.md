# Nuvexa Architecture

## Primary runtime: local-first

Nuvexa 1.0 now uses a phone-hosted architecture for its primary experience. The launcher opens Nuvexa Local directly and starts an embedded HTTP file service in the same Android application process. No external Nextcloud installation or separate server application is required for local operation.

### NUVEXA LOCAL SERVER

- `LocalDriveServerService`: foreground service declared as `connectedDevice`.
- `LocalDriveHttpServer`: embedded TCP/HTTP server using the Android/Java socket stack.
- `LocalDriveConfig`: local storage root, LAN address and pairing-token lifecycle.
- `LocalDrivePathPolicy`: canonical-path boundary and filename validation.
- `LocalDriveActivity`: native server/storage control surface.

The server binds to a local port from 8787 through 8797, advertises the current LAN URL only in the app, and requires the Nuvexa pairing token for file APIs. Uploads are written to a temporary file and validated by size before replacement.

The current target SDK is 36, where the INTERNET permission still grants LAN access. Before moving targetSdk to 37, Nuvexa must add and runtime-gate Android 17's ACCESS_LOCAL_NETWORK permission.

## Preserved client baseline

The repository preserves the mature Nextcloud Android architecture and codebase instead of destructively removing proven file, media, WebDAV and sync functionality. That code is no longer required to reach the default local experience and may later become an optional interoperability/cloud connector.

Nuvexa-specific boundaries:

- NUVEXA LOCAL SERVER - phone-hosted HTTP service and local storage.
- NUVEXA CORE - preserved account/server/WebDAV compatibility code.
- NUVEXA SYNC - persistent queue, transfer identity, verification, recovery and offline journal.
- NUVEXA PHOTOS - timeline, albums and viewer.
- NUVEXA MEDIA - playback isolated from transfer state.
- NUVEXA DIRECT / CONNECT - future device-to-device capabilities.
- NUVEXA SECURITY - local pairing credentials, biometric/app lock and future vault.
- NUVEXA STORAGE - local storage, cache analysis and exact-duplicate detection.

No module is allowed to weaken data-integrity guarantees merely to simplify UI work.
