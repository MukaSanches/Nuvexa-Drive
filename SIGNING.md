# Release Signing

Nuvexa production releases use one stable signing identity.

Rules:
- The private keystore and its password are never committed to this repository.
- CI may receive signing material only through an authorized secret store.
- Release tooling must verify the expected signing certificate fingerprint.
- A release must not be labelled signed unless the final APK has been verified.
- Google Play builds must follow Play App Signing requirements.
- Direct GitHub builds may use the same authorized signing identity but must not bypass Android package-installer security.

## Nuvexa 1.0 production identity

The first Nuvexa production signing identity was created before the 1.0 release candidate and is preserved outside this public repository.

Public certificate SHA-256 fingerprint:

`5C:A9:4F:0F:55:81:3D:DE:11:F5:FC:BC:3F:D8:86:01:38:08:34:AC:5B:44:8D:CB:CF:DB:D4:E4:FA:00:AC:D5`

This fingerprint is safe to publish and is the value release automation must compare against. The keystore bytes, alias password and key password are private release credentials and must never appear in Git history, build logs or public artifacts.

A private recovery backup is maintained separately from GitHub. Future releases must use this same authorized identity unless an Android-supported key-rotation procedure is deliberately performed and documented.
