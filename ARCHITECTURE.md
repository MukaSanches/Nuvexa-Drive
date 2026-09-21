# Nuvexa Architecture

The initial release preserves the mature Nextcloud Android architecture and adds Nuvexa changes incrementally.

Nuvexa-specific boundaries:

- NUVEXA CORE - accounts, server/WebDAV and metadata.
- NUVEXA SYNC - persistent queue, transfer identity, verification, recovery and offline journal.
- NUVEXA PHOTOS - timeline, albums and viewer.
- NUVEXA MEDIA - playback isolated from transfer state.
- NUVEXA DIRECT / CONNECT - future device-to-device capabilities.
- NUVEXA SECURITY - credentials, biometric/app lock and future vault.
- NUVEXA STORAGE - cache, storage analysis and exact-duplicate detection.

No module is allowed to weaken E2EE or data-integrity guarantees to simplify UI work.
