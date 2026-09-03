# Derived-app policy profile

Copy this file for every app built from the shell. Replace **every** `DECISION_REQUIRED` with an explicit answer, owner, and evidence link. `scripts/validate-shell.sh --strict` blocks release while any marker remains.

## Release identity

| Field | Value |
|---|---|
| App / package | DECISION_REQUIRED |
| Release version / commit | DECISION_REQUIRED |
| Store account / developer name | DECISION_REQUIRED |
| Countries / regions | DECISION_REQUIRED |
| Category / content rating | DECISION_REQUIRED |
| Target audience / age bands | DECISION_REQUIRED |
| Release owner and date | DECISION_REQUIRED |

## Functionality and SDK inventory

| Decision | Answer / evidence |
|---|---|
| Primary user utility and review path | DECISION_REQUIRED |
| All SDKs, versions, owners, purposes | DECISION_REQUIRED |
| Remote services / downloaded code / WebViews | DECISION_REQUIRED |
| Hardware and permissions | DECISION_REQUIRED |
| Offline behavior and degraded states | DECISION_REQUIRED |

## Data and accounts

| Decision | Answer / evidence |
|---|---|
| Data inventory and data-flow diagram | DECISION_REQUIRED |
| Data safety form compared with runtime | DECISION_REQUIRED |
| Privacy policy URL and in-app link | DECISION_REQUIRED |
| Collection disclosure / consent moments | DECISION_REQUIRED |
| Retention, deletion, export, incident contact | DECISION_REQUIRED |
| Account creation / login / in-app deletion / web deletion URL | DECISION_REQUIRED |
| Backup provider, data scope, encryption, conflicts, deletion | DECISION_REQUIRED |
| Children, precise location, health, finance, authentication data | DECISION_REQUIRED |

## Monetization

| Decision | Answer / evidence |
|---|---|
| Mode: free / ads / IAP / subscription / usage cap | DECISION_REQUIRED |
| Play Billing exception or alternative-billing program | DECISION_REQUIRED |
| Product IDs, types, plans/offers, periods, prices, territories | DECISION_REQUIRED |
| Trial/intro conversion and cancellation copy | DECISION_REQUIRED |
| Restore, pending, acknowledgement, refund, revoke, offline behavior | DECISION_REQUIRED |
| Paywall screenshot proves no logo/icon/brand-mark image | DECISION_REQUIRED |
| Ads flavor, consent, audience, SDK and privacy-choice evidence | DECISION_REQUIRED |
| Local usage-cap reset risk accepted or server/store-backed design | DECISION_REQUIRED |

## Conditional policy decisions

Replace each marker with `NO — not present` or `YES — <policy owner/evidence>`.

| Capability | Decision |
|---|---|
| Children or mixed audience | DECISION_REQUIRED |
| UGC, chat, social, dating, creator content | DECISION_REQUIRED |
| Generative AI | DECISION_REQUIRED |
| Health / medical | DECISION_REQUIRED |
| Finance / loans / trading / crypto | DECISION_REQUIRED |
| Gambling / real-money games / contests | DECISION_REQUIRED |
| News / politics / elections | DECISION_REQUIRED |
| Government information or services | DECISION_REQUIRED |
| Sensitive or restricted permissions/APIs | DECISION_REQUIRED |
| Background location / foreground services | DECISION_REQUIRED |
| VPN / accessibility / device admin | DECISION_REQUIRED |
| Accounts / cloud / optional backup | DECISION_REQUIRED |
| Ads / rewarded / interstitial / app-open | DECISION_REQUIRED |
| External offers / alternative billing | DECISION_REQUIRED |
| Hardware / Bluetooth / camera / OCR / media | DECISION_REQUIRED |
| Physical goods, donations, regulated products | DECISION_REQUIRED |
| Third-party IP, brands, licensed content | DECISION_REQUIRED |

## Console and review evidence

| Evidence | Link / owner |
|---|---|
| Signed artifact and SDK report | DECISION_REQUIRED |
| Store listing and screenshots | DECISION_REQUIRED |
| Content rating and target audience forms | DECISION_REQUIRED |
| Data safety, ads, app access and category declarations | DECISION_REQUIRED |
| Reviewer account/instructions and gated-feature path | DECISION_REQUIRED |
| UI regression matrix | DECISION_REQUIRED |
| Billing / restore / consent test evidence | DECISION_REQUIRED |
| Policy recheck date and sign-off | DECISION_REQUIRED |
