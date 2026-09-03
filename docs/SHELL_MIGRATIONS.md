# Shell contract migrations

The Android shell uses an explicit contract version so AI tools and derived apps can distinguish template behavior from product code. Never copy a new shell file blindly over a customized app.

## Current contract: 2.1.0

Migration from 2.0.x:

1. Set first-launch baseline to `OnboardingPresentation.None`, `showBrandMark = false`, and legal acceptance enabled. Opt back into education only when the app needs it.
2. Preserve the no-brand purchase rule across paywall, restore, win-back, introductory-offer, and promotional-purchase screens.
3. Add the paywall semantic tags and automated source/UI guard.
4. Add `BackupProvider`; keep `DisabledBackupProvider` unless the derived app explicitly authorizes cloud/account behavior and completes policy work.
5. Complete `APP_POLICY_PROFILE.md`, execute `UI_REGRESSION_MATRIX.md`, and resolve every conditional policy in `STORE_COMPLIANCE_CHECKLIST.md`.
6. Reconcile product IDs, subscription copy, privacy/terms URLs, Data safety, ads flavor, and reviewer instructions for the derived app.
7. Run strict validation locally on the release candidate. Workflows remain manual-only.

## Contract history

| Version | Change |
|---|---|
| 2.1.0 | Legal-only default, no-logo purchase invariant, compliance router/profile, optional disabled backup seam, paywall UI guard, expanded UI matrix |
| 2.0.0 | Native Compose feature-canvas boundary, adaptive navigation, billing verification, usage caps, ads/noAds flavors, 31-locale shared core |

## Merge discipline

- Treat `ShellConfig.kt`, `ShellApp.kt`, shell data contracts, validator, and compliance docs as locked infrastructure.
- Keep product behavior behind `FeatureCanvas` and app-specific adapters.
- Diff old/new contract behavior, migrate one concern at a time, and retain app-specific configuration.
- Do not weaken fail-closed purchase verification, legal gating, no-logo purchase checks, or strict profile blocking.
- Record the adopted contract version and migration evidence in the derived app’s release notes.
