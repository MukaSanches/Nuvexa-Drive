# Changelog

## 1.0.0 - development

### Base
- Imported stable Nextcloud Android 35.0.0 as the initial functional baseline.
- Set independent Nuvexa Drive application identity and provider authorities.
- Preserved automatic Android locale resources and RTL support.
- Added Nuvexa branding assets and independent source/issue links.

### Reliability
- Added active-transfer Network Guard so Wi-Fi-only uploads are paused if the active network changes to cellular or metered transport.
- Re-check charging-only and power-saver constraints while an upload is in progress.
- Added unit regression tests for network and power policy transitions.
- Added two-phase non-E2EE upload verification: remote metadata must confirm the exact expected byte length before destructive local actions can run.
- Added regression tests preventing non-empty sources from accepting zero-byte/truncated remote objects as complete.

### Release engineering
- Established the stable Nuvexa 1.0 production signing certificate identity.
- CI verifies release signing when authorized secrets are configured and will not label an unsigned artifact as a production release.
- Release artifacts include checksums and signature-verification evidence when signing is available.

### Quality
- Added Nuvexa build/validation documentation.
- Added CI bootstrap/build workflow.
- Reliability hardening items remain gated by regression tests and are not marked complete until validated.
