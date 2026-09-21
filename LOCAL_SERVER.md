# Nuvexa Local Server

## Goal

The phone is the server. Installing Nuvexa Drive is sufficient to create and operate the local file service; a separate Nextcloud app, Termux server, desktop daemon or cloud account is not part of the primary path.

## Runtime

When Nuvexa launches, `LocalDriveActivity` starts `LocalDriveServerService`. The service runs an embedded `ServerSocket` HTTP server and keeps a foreground notification visible while LAN access is active.

The service uses the Android foreground-service type `connectedDevice`, which matches interactions with other devices over a network connection. It does not use `dataSync` as the long-running server type because current Android versions impose time limits on data-sync foreground services.

## Authentication

A random 192-bit token is generated on-device and stored in private app preferences. The token is required through `X-Nuvexa-Token`, `Authorization: Bearer`, or the explicit pairing URL. Rotating the token invalidates previous pairing links.

The implementation intentionally does not place the token in logs.

## HTTP surface

- `GET /` local web portal
- `GET /api/status`
- `GET /api/files?path=...`
- `POST /api/upload?path=...&name=...` with the raw file body
- `POST /api/mkdir?path=...&name=...`
- `DELETE /api/file?path=...&name=...`
- `GET /file?path=...&name=...`

## Storage safety

The V1 local server uses Nuvexa private app storage. Path segments are normalized and rejected if they contain separators, control characters, dot traversal, or exceed the filename limit. Every resolved path is canonicalized and verified to remain below the Nuvexa root.

Uploads are streamed to a temporary file, fsynced, size-checked, then renamed or copied into the final destination. Interrupted uploads therefore do not replace a previously valid destination file.

## Network scope

The service is intended for trusted local networks. The V1 portal uses HTTP plus a high-entropy token rather than pretending a self-signed certificate provides browser-grade identity. Sensitive deployments should avoid untrusted public Wi-Fi.

Nuvexa currently targets SDK 36. Android documentation states that LAN access is implicitly covered by INTERNET for apps targeting SDK 36 or lower. Android 17 requires ACCESS_LOCAL_NETWORK for apps targeting SDK 37 or higher; the target-SDK upgrade must implement that runtime permission before shipping.
