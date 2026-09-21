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

## Implemented and regression-tested Nuvexa hardening

### Active transfer Network Guard

The upload worker now re-evaluates Wi-Fi-only, charging-only and power-saver policy while bytes are actively flowing, rather than only before the request begins. If Android changes from unmetered Wi-Fi to cellular/metered transport during an upload, the existing upload operation is cancelled with a retryable delayed-for-Wi-Fi result instead of silently continuing on mobile data.

The policy decision is isolated in `TransferPolicyGuard` and unit tests cover:

- unmetered Wi-Fi allowed;
- Wi-Fi to cellular transition blocked;
- metered Wi-Fi blocked for Wi-Fi-only;
- charging-only interrupted when power is removed;
- automatic upload blocked in power saver;
- explicit user upload allowed to ignore power saver when configured by the existing upload semantics.

These items are requirements. Individual mechanisms are only marked implemented after code and regression tests exist.
