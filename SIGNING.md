# Release Signing

Nuvexa production releases use one stable signing identity.

Rules:
- The private keystore and its password are never committed to this repository.
- CI may receive signing material only through an authorized secret store.
- Release tooling must verify the expected signing certificate fingerprint.
- A release must not be labelled signed unless the final APK has been verified.
- Google Play builds must follow Play App Signing requirements.
- Direct GitHub builds may use the same authorized signing identity but must not bypass Android package-installer security.
