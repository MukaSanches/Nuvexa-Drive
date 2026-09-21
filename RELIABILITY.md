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
