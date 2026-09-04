# Welding Gas Wallet Android guide

This is the authoritative native Android application. It derives from `lrodeveloperr/Android-shell`; do not replace it with Flutter, React Native, a WebView shell, or code from the obsolete Flutter repository.

## Product contract

- The approved browser preview is the visual and behavioral source of truth.
- Keep Material 3 navigation, sheets, icons, keyboards, safe areas, accessibility, RTL, and adaptive phone/tablet behavior native.
- Free users have at most three active cylinders and a permanently reserved lower banner area.
- A verified monthly subscription removes both the banner and cylinder limit.
- Cylinder records stay on-device. Native file export/import is optional and purchase entitlement is never included.
- Display currency signs in the interface. Persist ISO currency codes only in the data layer; never silently convert or combine currencies.
- Preserve delete confirmation, 15-second Undo, Return/Archive, activity history, reminders, suppliers, search/filters, and the data-entry friction reductions.
- Privacy Policy and Terms are maintained externally. Do not invent policy copy.

## Source map

- Product model and persistence: `app/src/main/java/com/goodusestudios/weldinggaswallet/wallet/WalletStore.kt`
- Product screens: `app/src/main/java/com/goodusestudios/weldinggaswallet/wallet/WalletFeature.kt`
- Reminder delivery: `app/src/main/java/com/goodusestudios/weldinggaswallet/wallet/ReminderReceiver.kt`
- App/navigation shell: `app/src/main/java/com/goodusestudios/weldinggaswallet/ui/ShellApp.kt`
- Monetization/legal/destinations: `app/src/main/java/com/goodusestudios/weldinggaswallet/ui/ShellConfig.kt`
- Theme: `app/src/main/java/com/goodusestudios/weldinggaswallet/ui/ShellTheme.kt`
- Ads/consent: `ui/AdBanner.kt`, `data/AdConsentController.kt`
- Billing: `data/BillingController.kt`, `data/AccessPolicy.kt`
- Validation: `scripts/validate-shell.sh`
- Reliability audit: `docs/RELIABILITY_AUDIT.md`

## Reliability and touchscreen gate

- Treat every visible control as one full-surface target. Touch targets must be at least 48×48 dp; full rows must own their click/select semantics, while nested `RadioButton`/icons use `onClick = null` so taps are not split into competing regions.
- Exercise controls through their semantics nodes in Compose UI tests, including edge taps, minimum bounds, disabled states and exactly-once callbacks. A label existing on screen is not proof that its parent control works.
- Enforce the three-active-cylinder/free-managed policy inside `WalletStore` for add, duplicate, edit, status, service, reminder, restore and Undo. Pro expiry with excess data requires selection of three managed records; all other active records remain visible and read-only.
- Every mutation must validate first and persist transactionally before publishing success. Use a durable write result, roll back on failure, and surface an error; never rely on fire-and-forget `SharedPreferences.apply()` for wallet records.
- Strictly decode and validate local/backup JSON: unique IDs/serials, valid references, finite positive capacities, known units/currencies and nonnegative costs. Preserve a recovery copy of damaged local data; a malformed import must never become an empty wallet.
- Restore must sort activity, cancel alarms for removed records and reschedule only valid active reminders. Alarm state must be rebuilt after boot, app replacement and clock/time-zone changes. Never save a past reminder or claim success when permission, scheduling or persistence fails.
- Internal cylinder and supplier detail screens must consume system Back before the shell exits or changes top-level route. User-entered form state should use `rememberSaveable` where supported.
- Billing connect, restore and purchase actions are single-flight. Keep at most one queued ready action, disable purchase for current owners, expose product-query failure, and make retry/restore controls full-size.
- A reliability fix is incomplete without a store regression test and a Compose semantics test for the affected control. Run `bash scripts/validate-shell.sh`; run unit/instrumentation tests when the Android toolchain is available.

## Release control

- All GitHub workflows must remain `workflow_dispatch` only.
- Never run or dispatch APK, bundle, or paid hosted build workflows without explicit user authorization.
- Production ads require publisher-owned AdMob app/banner IDs.
- Production purchase verification requires the Play licensing public key and the matching monthly product.
- Run `bash scripts/validate-shell.sh` for code-only structural checks.
