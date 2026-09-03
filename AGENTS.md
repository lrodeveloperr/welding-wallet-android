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

## Release control

- All GitHub workflows must remain `workflow_dispatch` only.
- Never run or dispatch APK, bundle, or paid hosted build workflows without explicit user authorization.
- Production ads require publisher-owned AdMob app/banner IDs.
- Production purchase verification requires the Play licensing public key and the matching monthly product.
- Run `bash scripts/validate-shell.sh` for code-only structural checks.
