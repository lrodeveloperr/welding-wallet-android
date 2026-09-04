# Welding Gas Wallet — Android

Native Kotlin and Jetpack Compose implementation of the approved Welding Gas Wallet MVP for Android phones and tablets. Flutter is not used.

The app is an offline, cylinder-only wallet with search and status filters, refill/exchange/cost history, suppliers, local reminders, duplicate entry, 15-second deletion undo, archive/return, native backup files, 30 selectable languages, automatic-region currency with manual override, a three-active-cylinder free limit, an anchored lower ad banner, and an annual Play subscription that removes the banner and limit. The United States base price is US$19.99 per year; Google Play owns geographic pricing and the app displays Play's localized price, currency, and billing period.

Currency values are shown with locale-appropriate signs such as `$`, `£`, `€`, `¥`, or `₹`. ISO currency codes remain internal so historical transactions can stay normalized and currencies are never silently converted or combined.

The app derives from `lrodeveloperr/Android-shell`; the reusable shell repository remains unchanged. GitHub Actions are manual-only and do not run on a code push. Production packaging requires publisher-owned AdMob identifiers, the Play licensing public key, and `com.gooduse.weldinggaswallet.pro.yearly` in Play Console with exactly one auto-renewing one-year base plan, a US$19.99 United States base price, and reviewed regional prices for every enabled country.

Local structural validation:

```bash
bash scripts/validate-shell.sh
```

Opening or building the project requires current Android Studio, JDK 17, Android SDK 36, and Gradle 8.13. Do not trigger the manual APK workflow unless its cost is explicitly authorized.
