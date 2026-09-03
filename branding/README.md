# Brand asset contract

Replace the entire icon family together for every derived app:

- `branding/play-store-icon-512.png` — the 512 × 512 Play Console icon (“thumbnail”)
- `branding/play-store-icon.svg` — editable master retained in source control
- `drawable/ic_launcher_foreground.xml` — adaptive foreground inside the 66 × 66 safe zone
- `drawable/ic_launcher_foreground.xml` — app and notification icon artwork
- `drawable/ic_brand_mark.xml` — in-app onboarding mark
- `values/colors.xml` — launcher and splash background

The manifest, round icon, Android 13 monochrome icon, SplashScreen API, and onboarding header are already connected. Run `scripts/validate-shell.sh --strict` before a derived release; it rejects the template identity and example identifiers.
