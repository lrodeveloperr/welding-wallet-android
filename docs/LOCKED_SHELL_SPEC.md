# Locked Android Shell Specification

This is the shell’s operating contract. A derived app changes facts and product features; it does not rebuild generic flows.

## Boundary

| Locked by the shell | Changed per app |
|---|---|
| Adaptive phone/tablet content geometry, insets, reflow, and large-text fallback | Feature screen content and app-specific list/detail layouts |
| Bottom navigation/rail promotion, top app bar, ad placement, back behavior | Destination IDs, labels, and icons (one to five) |
| Pager, single-page, or disabled onboarding renderer | Onboarding page copy, count, and optional brand mark |
| Versioned onboarding persistence and legal re-consent | Reviewed legal text, URLs, effective date, and version |
| Adaptive, round, monochrome, splash, in-app, and 512 px store-icon slots | The actual representative app artwork and launcher color |
| Product query, offer selection, Play flow, restore, product-scoped entitlement state, persistent usage metering, and acknowledgement | Play Console products, IDs, benefits, prices/offers, cap, grace period, and trusted verifier |
| UMP consent update, ad-request gate, and privacy-options entry point | AdMob app/unit IDs, account messages, audience/age classification |
| Locked 31-locale shared-core bundle, resolver precedence, picker, and locale config | App/domain localization delta |
| Loading, empty, error/retry, populated, paywall, legal, settings, and component-lab states | Domain-specific actions and data |
| CI structural checks and unit tests | Strict release-mode replacement of all template values |

## One-file customization

Edit `ShellConfig.kt` first. It contains the brand, onboarding presentation and pages, legal version/documents, monetization mode/products/benefits, ad IDs, and destinations. `ShellConfig.validationErrors()` catches inconsistent definitions.

Onboarding supports:

- `Pager`: a short value tour followed by explicit legal acceptance.
- `SinglePage`: one concise value proposition plus acceptance.
- `None`: bypass for products where first-run education is unnecessary.
- A separate legal-update gate: increasing `legal.version` requests only the new acceptance from returning users.

Terms acceptance is not advertising consent and is not a substitute for a prominent disclosure about unexpected personal/sensitive-data use. Permissions should be requested contextually when a feature needs them, following [Android onboarding guidance](https://developer.android.com/design/ui/mobile/guides/patterns/onboarding) and the [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311).

## Scaling contract

Content—not shell chrome—adapts. Width classes are compact below 600 dp, medium from 600 dp, and expanded from 840 dp. Onboarding content is capped at 560 dp and upper-anchored on regular-height screens; it becomes scrollable below 700 dp or above 1.3× font scale. Main feature content begins 8 dp below the app bar. Expanded screens use list/detail space rather than stretching a phone column. This follows Android’s [window-size-class guidance](https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes).

## Feature canvas contract

The host passes one Compose `FeatureCanvas` to `ShellApp`. The shell checks access before composing it. A feature reports `reportSuccessfulAction(stableActionId)` only after the useful domain operation commits successfully; the persistent ledger deduplicates retries and is bounded by the configured cap. The feature may request the paywall but cannot grant itself entitlement.

## Purchase contract

The controller uses Play Billing 9.1.0 to query one-time products and subscriptions, display Play-provided price/eligibility, launch the selected flow, represent pending purchases, restore active purchases, and acknowledge only verified purchases. It rejects unconfigured product IDs and refreshes on resume. The built-in verifier checks the Play signature with the configured licensing public key and rejects blank/invalid keys; an asynchronous trusted-service `PurchaseVerifier` can be injected. Google recommends secure backend processing in its [billing integration guide](https://developer.android.com/google/play/billing/integrate).

Successful authoritative queries replace cached entitlements, which applies refunds and revocations. Offline query failure preserves a valid cache. One-time purchases cache until the next authoritative query; subscriptions use a configurable 0–168 hour grace window.

## Advertising contract

UMP 4.0.0 refreshes consent status at every launch. Ads initialize and load only when `canRequestAds()` is true. Settings exposes “Ad privacy choices” whenever UMP says an entry point is required. Configure the privacy message in AdMob; code alone cannot create the account-side message. See Google’s [UMP setup](https://developers.google.com/admob/android/privacy).

Use the `noAds` flavor for an ad-free artifact. It removes the `AD_ID` permission and Mobile Ads initializer from the merged manifest and does not initialize UMP or Mobile Ads. The `ads` flavor supplies the AdMob application metadata.

## Brand and release gate

Replace every file listed in `branding/README.md` as one change. The adaptive icon follows the required foreground/background design and includes an Android 13 monochrome layer; the system splash and in-app onboarding mark are connected. The Play Console’s icon is metadata outside the APK, so the repository holds a checked 512 × 512 export to prevent it being forgotten.

Run:

```bash
bash scripts/validate-shell.sh --strict ads # or: --strict noAds
gradle testAdsDebugUnitTest lintAdsDebug assembleAdsDebug assembleNoAdsDebug
```

Strict validation intentionally fails until the app name, support/legal values, products, and Google demo ad ID have been replaced. This is the safety rail that turns customization omissions into build-time failures.

The manual GitHub workflow runs the same structural check, unit suite, lint task, and APK assemblies on a clean runner without consuming hosted minutes on every push or pull request.

## Source basis

The decisions above are grounded in Android’s official guidance for [adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen), [per-app languages](https://developer.android.com/guide/topics/resources/app-languages), [app architecture](https://developer.android.com/topic/architecture), [DataStore](https://developer.android.com/topic/libraries/architecture/datastore), and [Play Billing](https://developer.android.com/google/play/billing/integrate).
