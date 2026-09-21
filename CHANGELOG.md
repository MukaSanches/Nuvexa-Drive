# Changelog

## 1.0.0 - development

### Base
- Imported stable Nextcloud Android 35.0.0 as the initial functional baseline.
- Set independent Nuvexa Drive application identity and provider authorities.
- Preserved automatic Android locale resources and RTL support.
- Added Nuvexa branding assets and independent source/issue links.

### Reliability hardening
- Added two-phase verification for normal uploads before destructive local post-upload actions.
- Added zero-byte/truncation guard using verified remote size.
- Added integrity gate before temporary downloads are promoted to final files.
- Re-evaluate Wi-Fi-only policy during active uploads.
- Added focused unit tests for transfer integrity and network policy.

### Product identity
- Isolated QA and Dev account/provider authorities under the Nuvexa package identity.
- Removed inherited Nextcloud Firebase/push credentials from the Google Play flavor; Nuvexa-owned infrastructure is required before Play push is enabled.
- Removed misleading upstream beta/download/runtime links from Nuvexa configuration.

### Quality
- Added Nuvexa build/validation documentation.
- Added CI bootstrap/build workflow.
- Reliability hardening items remain gated by regression tests and are not marked complete until validated.
