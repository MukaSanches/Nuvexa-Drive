# Nuvexa Reliability Contract

Target Reliability Engine:

Persistent Queue -> Transfer Identity -> Network Guard -> Chunk/Resume -> Server Verification -> Commit -> Recovery.

Mandatory invariants:
- UI/activity lifetime cannot be the source of truth for transfer completion.
- Before retrying a failed completion response, verify whether the remote object already exists and is complete.
- Do not mark a non-empty source as successfully uploaded to a zero-byte remote object.
- Wi-Fi-only policy is re-evaluated during transfers, not only before they start.
- Retry is bounded with backoff/circuit-breaker behavior.
- Partial downloads remain temporary until verification and atomic completion.
- Startup/reboot recovery inspects interrupted work rather than silently abandoning it.
- Reinstall/re-login reconciliation must prevent blind re-upload of an existing library.

These items are requirements. Individual mechanisms are only marked implemented after code and regression tests exist.


## Implemented in the current hardening branch

- **Two-phase normal upload completion:** transport success is followed by a remote metadata read and exact byte-length verification before local delete/move/copy behavior is allowed.
- **Zero-byte/truncation guard:** a non-empty source cannot pass verification against a zero-byte or truncated remote object.
- **Download promotion guard:** a known-size download remains temporary unless its byte length matches the expected remote size. E2EE continues to rely on authenticated decryption.
- **In-flight Wi-Fi guard:** Wi-Fi-only is checked during transfer progress and the operation is cancelled into a delayed-for-Wi-Fi state if the network changes.
- **Focused tests:** pure policies cover exact/zero/truncated sizes and Wi-Fi/mobile/metered transitions.

These items are still release-gated until the complete CI and device/server validation matrix passes.
