# UI regression matrix

Run this matrix on the exact release configuration. Record device/emulator, OS, locale, font scale, color mode, build flavor, result, screenshot/video, and issue link. Source-level guards supplement this matrix; they do not replace it.

## Required environments

| Dimension | Minimum coverage |
|---|---|
| Compact phone | Small supported width and current Android |
| Large phone | Tall/narrow and short/wide windows |
| Tablet / foldable | Expanded width, fold/unfold or resize |
| Text | 100%, 130%, 160%, and maximum supported font scale |
| Direction | English LTR plus Arabic or Hebrew RTL |
| Locale | Long-string locale plus one CJK locale; verify all 31 bundled locales resolve |
| Appearance | Light, dark, dynamic color on/off, high contrast where supported |
| Input/accessibility | Touch, keyboard/D-pad if supported, TalkBack traversal/labels |

## Screen and state checks

| Surface | Required states and assertions |
|---|---|
| First launch | Legal-only default; Privacy and Terms open; acceptance is explicit and persists |
| Optional onboarding | Disabled/single/pager, short/long copy, logo only on education screens when configured |
| Feature canvas | Populated, empty, loading, error, retry, offline, access denied |
| Navigation | Bottom bar, rail, back/up, rotation/resize, selected destination |
| Settings | Upgrade, language, privacy choices when required, support, legal, debug-only lab |
| Language | System default plus every supported locale; restart/state preservation |
| Paywall | Loading, ready, unavailable, pending, entitled, subscription, one-time, retry |
| Purchase controls | Price/period, renewal or non-recurring copy, purchase, restore, Privacy, Terms |
| Purchase identity rule | **No app logo, launcher icon, brand mark, or decorative app identity artwork anywhere on paywall/purchase/restore/win-back/promo surfaces** |
| Ads | Ads and noAds flavors, consent denied/accepted, privacy choices, entitlement removal, no overlay |
| Backup | Hidden with default provider; create/restore/progress/error with an authorized provider |
| Legal update | New legal version blocks canvas until accepted; published links work |
| Process/lifecycle | Background/foreground, process death, offline restart, interrupted purchase |

## Accessibility and layout assertions

- [ ] No clipped, overlapped, truncated-critical, or unreachable content.
- [ ] Scrolling exposes all controls; IME and system bars do not cover actions.
- [ ] Focus order follows reading order and survives adaptive layout changes.
- [ ] Icons have meaningful labels when actionable; decorative images are hidden from accessibility.
- [ ] Touch targets, contrast, headings, selection state, errors, and progress are perceivable.
- [ ] RTL mirrors directional layout without mirroring non-directional symbols.
- [ ] Purchase and legal copy remains readable at maximum text size.
- [ ] The anchored ad banner never overlays canvas or navigation.
- [ ] Screenshot comparison uses stable data and masks only genuinely dynamic store values.
