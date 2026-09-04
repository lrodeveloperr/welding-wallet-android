# Reliability audit and regression guidance

This checklist records the failure modes fixed in the native Android shell. Future changes must preserve the Material 3 design and keep these guarantees.

## Resolved failure modes

| Area | Failure | Required behavior |
| --- | --- | --- |
| Touch | Radio rows and some labels responded only in a small region | The full row owns one click action, nested radio/icon controls do not compete, and targets are at least 48×48 dp |
| Storage | `SharedPreferences.apply()` reported success before a durable write | Persist with a checked transaction, then publish state; retain prior state and show an error when the write fails |
| Recovery | Damaged local JSON silently became an empty wallet | Preserve the original under a recovery key and report the problem |
| Restore | A valid header with malformed records could erase the wallet | Strictly decode and validate the complete snapshot before any mutation |
| Access | UI-only checks let restore, Undo and direct mutations bypass the free limit | Enforce lifecycle and the three-managed-cylinder policy in `WalletStore` and require selection after Pro expiry/large restore |
| Input | Unit-only exchanges, non-finite capacities and oversized costs could corrupt or crash a mutation | Reject invalid and out-of-range values before conversion or persistence |
| Reminders | Alarms vanished after reboot, past/denied reminders appeared saved, and removed records could still notify | Rebuild after reboot/update/time changes; require permission/future time; cancel old alarms before restored alarms |
| Navigation | System Back could exit from an internal detail and Settings children jumped to Main | Detail screens consume Back and shell child routes return to Settings |
| Billing | Repeated taps queued duplicate actions and owners could purchase again | Keep one queued action, use single-flight state, disable owned purchases and expose product-query retry |
| Undo/forms | Undo stayed visible after expiry and form input disappeared after recreation | Expire the banner on time and save supported input state with `rememberSaveable` |

## Required verification

- Run `bash scripts/validate-shell.sh` and require a clean result.
- Run JVM and instrumentation tests on an available Android toolchain.
- In Compose UI tests, assert semantics, at least 48 dp height, edge taps, disabled states and exactly-once callbacks.
- Exercise add, edit, duplicate, status, refill, exchange, cost, reminder, return, archive, delete, Undo, backup, restore, language, currency, legal links, purchase, restore purchase and retry with TalkBack and large font scaling.
- Re-test durable-write failure, malformed restore, Pro expiry above three active cylinders, reboot alarm recovery, notification denial and rapid repeated billing taps.
