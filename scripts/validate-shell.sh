#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fail() { echo "VALIDATION FAILED: $*" >&2; exit 1; }
require_file() { [[ -s "$1" ]] || fail "Missing $1"; }
require_text() { grep -Fq "$2" "$1" || fail "$1 must contain: $2"; }

manifest="$root/app/src/main/AndroidManifest.xml"
config="$root/app/src/main/java/com/goodusestudios/weldinggaswallet/ui/ShellConfig.kt"
feature="$root/app/src/main/java/com/goodusestudios/weldinggaswallet/wallet/WalletFeature.kt"
store="$root/app/src/main/java/com/goodusestudios/weldinggaswallet/wallet/WalletStore.kt"
settings="$root/app/src/main/java/com/goodusestudios/weldinggaswallet/ui/ShellScreens.kt"

for file in "$manifest" "$config" "$feature" "$store" "$settings" "$root/app/build.gradle.kts" "$root/app/src/main/java/com/goodusestudios/weldinggaswallet/ui/AdBanner.kt" "$root/README.md"; do require_file "$file"; done

if rg -n '(io\.flutter|FlutterActivity|flutter:|ReactNative|RCTRootView)' "$root/app" "$root/build.gradle.kts" "$root/settings.gradle.kts"; then
  fail 'Cross-platform runtime detected; this app must remain native Jetpack Compose'
fi

require_text "$root/app/build.gradle.kts" 'applicationId = "com.goodusestudios.weldinggaswallet"'
require_text "$config" 'initialMode = MonetizationMode.AdsWithSubscription'
require_text "$config" 'com.gooduse.weldinggaswallet.pro.yearly'
require_text "$config" 'Icons.Outlined.PropaneTank'
require_text "$feature" 'activeCylinders.size >= 3'
require_text "$feature" 'Search cylinders'
require_text "$feature" 'Duplicate cylinder'
require_text "$store" 'fun currencySign(code: String)'
require_text "$store" 'fun deleteAllData()'
require_text "$store" 'now() + 15_000'
require_text "$settings" 'Type DELETE to confirm'
require_text "$settings" 'walletStore.currencySign(walletStore.defaultCurrency)'
require_text "$root/app/src/main/java/com/goodusestudios/weldinggaswallet/ui/ShellApp.kt" 'AdaptiveAdBanner(canRequestAds = canRequestAds)'

language_count="$(sed -n '/val choices = remember/,/) }/p' "$settings" | grep -o '"[^"]*" to "[^"]*"' | wc -l | tr -d ' ')"
[[ "$language_count" == "30" ]] || fail "Expected 30 language choices; found $language_count"
! grep -Fq 'Follow system' "$settings" || fail 'Language selector must not contain Follow system'

for workflow in "$root"/.github/workflows/*.yml; do
  require_text "$workflow" 'workflow_dispatch:'
  if rg -n '^\s+(push|pull_request|schedule):' "$workflow"; then fail "$workflow must remain manual-only"; fi
done

grep -q 'android:icon="@mipmap/ic_launcher"' "$manifest" || fail 'Launcher icon missing'
grep -q 'android:roundIcon="@mipmap/ic_launcher_round"' "$manifest" || fail 'Round launcher icon missing'

echo 'Welding Gas Wallet Android validation passed.'
