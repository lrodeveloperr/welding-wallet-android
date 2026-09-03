# Google Play compliance checklist

Verified against official Google policy sources on **2026-09-02**. Re-check the linked sources at every release; Google can change policy, forms, deadlines, and target API requirements.

This is a release gate for the reusable Android shell. It is not a promise of approval. A derived app activates additional rules through its functionality, SDKs, audience, metadata, category, territories, and business model.

## Canonical policy sources

- [Developer Program Policy index](https://support.google.com/googleplay/android-developer/answer/17517561?hl=en)
- [Developer Distribution Agreement](https://play.google.com/about/developer-distribution-agreement.html)
- [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Data safety form](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [Payments policy](https://support.google.com/googleplay/android-developer/answer/10281818?hl=en)
- [Ads policy](https://support.google.com/googleplay/android-developer/answer/9857753?hl=en)
- [Permissions and APIs that access sensitive information](https://support.google.com/googleplay/android-developer/answer/16558241?hl=en)
- [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Prepare for review](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en)
- [Store listing guidance](https://support.google.com/googleplay/android-developer/answer/13393723?hl=en)
- [Policy announcements](https://support.google.com/googleplay/android-developer/answer/16926792?hl=en)

## 1. Shell and product quality

- [ ] The release is native Kotlin + Jetpack Compose and contains no placeholder, broken, misleading, hidden, or dormant functionality.
- [ ] Every visible control works; empty/loading/error/offline states are intentional and recoverable.
- [ ] The app delivers enough durable utility to stand alone and is not repetitive, spammy, or a thin wrapper.
- [ ] Navigation, back behavior, edge-to-edge insets, rotations, foldable/tablet layouts, RTL, large text, TalkBack labels, contrast, and touch targets pass the UI matrix.
- [ ] Crashes, ANRs, startup failures, excessive battery/network use, and unsupported-device paths have been reviewed using release telemetry or local evidence.
- [ ] The current Play target API requirement is satisfied. At the verification date, new phone/tablet submissions and updates require target API 36.
- [ ] All SDKs, libraries, native code, and downloaded code comply; no dynamic code bypasses Play review or changes the app’s primary purpose after review.
- [ ] Malware, deceptive behavior, device/network abuse, unauthorized interference, and unwanted-software rules have been checked.

## 2. Identity, metadata, intellectual property, and promotion

- [ ] App name, package name, developer identity, icon, screenshots, video, short/full descriptions, category, tags, contact details, and privacy URL are accurate and current.
- [ ] Store assets depict the actual release build and do not promise unavailable features, rankings, prices, awards, relationships, or endorsements.
- [ ] No impersonation, trademark confusion, copyright infringement, counterfeit goods, or unauthorized third-party content/assets exist.
- [ ] Metadata has no irrelevant keywords, testimonials, promotional pricing claims, ranking manipulation, or prohibited formatting.
- [ ] Content rating, target audience, ads declaration, app access instructions, news declaration, health declaration, financial features declaration, and all other Play Console declarations match the binary.
- [ ] Review credentials and steps reach every gated feature; MFA, location, hardware, or paid-access requirements are explained.
- [ ] Incentivized installs/reviews, rating manipulation, deceptive redirects, and affiliate spam are absent.

## 3. Privacy, data, accounts, and permissions

- [ ] A data inventory covers the app and every SDK: collected/shared data, purpose, processing location, retention, deletion, encryption, linkage, optionality, and age handling.
- [ ] The Data safety form exactly matches runtime behavior, including data collected by third-party SDKs.
- [ ] The published privacy policy is public, readable, non-geofenced, linked in Play Console and in-app, names the app/developer, and explains collection, use, sharing, security, retention, and deletion.
- [ ] Sensitive data access is necessary for a current user-facing feature, requested in context, minimized, securely handled, and never sold or used outside consent.
- [ ] Prominent in-app disclosure and affirmative consent appear before collection whenever access/use is not within reasonable user expectation.
- [ ] Runtime permissions are minimized and degrade gracefully when denied. Restricted permissions/APIs have the required core-function eligibility and Play declaration/approval.
- [ ] Background location, all-files access, exact alarms, package visibility, SMS/call log, accessibility, VPN, photos/video, health, contacts, microphone, camera, Bluetooth, notifications, and device identifiers are each separately justified if present.
- [ ] Account creation, sign-in, export, logout, deletion, retention, and deletion-request paths match the User Data policy. If account creation exists, account deletion is available both in-app and through the required web resource.
- [ ] Children’s data, precise location, advertising identifiers, and authentication/financial/health data receive their stricter applicable controls.
- [ ] TLS, secret handling, log redaction, backups, screenshots, clipboard use, WebView bridges, exported components, deep links, and local storage have been security-reviewed.
- [ ] The optional backup provider remains disabled unless its data flows, retention, deletion, account requirements, encryption, restore conflicts, and Data safety answers are completed.

## 4. Payments, subscriptions, and purchase surfaces

- [ ] Google Play Billing is used for in-app digital goods/services unless a documented exception or enrolled alternative-billing program applies.
- [ ] Product IDs, type, base plan/offer, territories, tax, eligibility, trial, introductory pricing, billing period, grace/account-hold behavior, and backend entitlement mapping match Play Console.
- [ ] Price and billing period come from Google Play, not hard-coded release claims.
- [ ] Subscription terms clearly disclose what is provided, renewal frequency, full recurring price, auto-renewal, cancellation path, trial conversion, and material limitations before purchase.
- [ ] One-time purchases are explicitly described as non-recurring.
- [ ] Restore/re-query, pending purchase, acknowledgement/consumption, revocation, refund, cancellation, offline grace, and multi-device behavior are tested.
- [ ] Entitlement is never granted from an unverified callback, UI flag, demo state, or stale indefinite cache.
- [ ] The paywall, subscription selector, purchase confirmation, restore, win-back, introductory-offer, and promotional-purchase surfaces contain **no app logo, launcher icon, brand-mark image, or decorative app identity artwork**.
- [ ] Purchase controls, Restore purchases, Privacy, Terms, price, product name, and required recurring/non-recurring disclosure remain visible and accessible.
- [ ] Dark patterns, forced urgency, fake discounts, obstructed cancellation, disguised purchases, and unclear buttons are absent.
- [ ] Physical goods/services, donations, peer-to-peer payments, regulated products, and other billing exceptions are classified and documented before implementation.

## 5. Ads and monetization conduct

- [ ] Select the `ads` flavor only when ads are part of the derived app; use `noAds` otherwise.
- [ ] The `noAds` flavor contains no advertising ID declaration and never initializes AdMob or UMP.
- [ ] Consent/choice is obtained where required before ad requests, and privacy choices remain reachable later.
- [ ] Ads are clearly distinguishable, do not imitate system/app controls, do not interfere with navigation/content, and are dismissible when policy requires.
- [ ] Interstitial, rewarded, app-open, lock-screen, disruptive, deceptive, and made-for-children ad restrictions are separately checked if introduced.
- [ ] Ad content rating and SDK configuration match target audience; children/family ads use only eligible SDKs and required treatment.
- [ ] The anchored optional banner never overlays content/navigation and disappears for paid removal where promised.
- [ ] Ads declaration, Data safety answers, privacy policy, consent flow, and store listing agree.

## 6. Content and conduct

- [ ] Prohibited/restricted content has been checked: sexual content, child endangerment, hate, violence, terrorism, dangerous products/activities, bullying/harassment, drugs, alcohol/tobacco, weapons, gambling, illegal activity, and inappropriate financial solicitation.
- [ ] Misrepresentation, manipulated media, deceptive claims, misinformation in regulated/high-risk areas, and exploitative behavior are absent or handled under the applicable policy.
- [ ] User-generated or social content, if present, has terms acceptance, reporting, blocking, moderation, enforcement, contact, and objectionable-content controls.
- [ ] AI-generated content, if present, has required safeguards, reporting/flagging, restricted-content prevention, and accurate disclosures.
- [ ] News, government information, elections/politics, health, finance/loans, crypto, gambling/games, dating, and educational claims are routed through their dedicated policies.

## 7. Conditional policy router

Mark every row in `APP_POLICY_PROFILE.md`. A “yes” activates additional official requirements and console declarations.

| Capability | Required policy work |
|---|---|
| Children or mixed audience | Families policy, target-audience accuracy, child data/ads/SDK rules |
| UGC, messaging, social, dating | UGC moderation, safety, reporting/blocking, abuse response |
| Generative AI | AI-generated content policy, safety filters, reporting |
| Accounts or cloud/backup | Account deletion, web deletion resource, data retention/export/security |
| Sensitive/restricted permissions | Core-function eligibility, prominent disclosure, declaration/approval |
| Health/medical | Health apps policy, declarations, evidence, disclaimers, regulated status |
| Finance, loans, trading, crypto | Financial services policies, licensing, disclosures, geography |
| Gambling/real-money games | Eligibility, licensing, age/geography controls, declarations |
| News/magazines | News policy and publisher/contact/content requirements |
| Government information | Clear source/affiliation and government-services policy |
| Ads | Ads policy, consent, SDK eligibility, audience treatment |
| Alternative billing/external offers | Program enrollment, eligible market/app, mandated UX/reporting/fees |
| Hardware, camera, OCR, Bluetooth | Permission minimization, hardware fallbacks, safety/privacy disclosures |
| Location/background work | Location eligibility, disclosure/consent, foreground service rules |
| VPN/accessibility/device admin | Dedicated core-function policy, declarations, non-deceptive use |
| Downloaded code or WebView | Code integrity, SDK compliance, no review bypass |

Dedicated official policies: [Families](https://support.google.com/googleplay/android-developer/answer/9893335?hl=en), [UGC](https://support.google.com/googleplay/android-developer/answer/9876937?hl=en), and [AI-generated content](https://support.google.com/googleplay/android-developer/answer/14094294?hl=en).

## 8. Final evidence pack

- [ ] `APP_POLICY_PROFILE.md` contains no `DECISION_REQUIRED`.
- [ ] Strict shell validation passes on the exact release commit.
- [ ] UI matrix evidence is attached for the actual configuration and supported form factors.
- [ ] Privacy policy, Terms, support URL/email, deletion URL, and review instructions are live.
- [ ] Store declarations and listing were compared with the signed artifact and SDK inventory.
- [ ] Billing/ad sandbox evidence covers every enabled product and failure state.
- [ ] Release notes explain user-visible changes accurately.
- [ ] Policy pages and announcements were rechecked on the submission date.
- [ ] A human release owner signed off; unresolved items block submission.
