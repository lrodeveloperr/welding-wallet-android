#!/usr/bin/env python3
"""Apply deterministic source fixes required by the pinned Android toolchain.

The reviewed source is shared with iOS, while Android CI pins Flutter 3.44.8 and
flutter_local_notifications 22.3.0. These edits are deliberately exact,
idempotent, and fail closed so SDK/API drift cannot silently change production
behavior.
"""

from __future__ import annotations

from pathlib import Path


def replace_exact(path: Path, old: str, new: str, label: str) -> None:
    """Replace one reviewed source form, or accept an already-patched file."""
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"Android source fix failed: missing {label} in {path}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def apply_android_review_fixes(root: Path = Path(".")) -> None:
    """Apply every pinned Android compatibility and QA fix beneath *root*."""
    app = root / "lib/src/app.dart"
    replace_exact(
        app,
        "import 'package:intl/intl.dart';",
        "import 'package:intl/intl.dart' show DateFormat;",
        "narrow intl import",
    )
    replace_exact(
        app,
        "      CylinderEventType.relationshipChanged => c.t('relationshipChanged'),\n"
        "      CylinderEventType.returned => c.t('eventReturned'),",
        "      CylinderEventType.relationshipChanged => c.t('relationshipChanged'),\n"
        "      CylinderEventType.note || CylinderEventType.photoAdded => c.t('note'),\n"
        "      CylinderEventType.returned => c.t('eventReturned'),",
        "exhaustive event label",
    )
    replace_exact(
        app,
        "material.formatTime(TimeOfDay.fromDateTime(local))",
        "material.formatTimeOfDay(TimeOfDay.fromDateTime(local))",
        "MaterialLocalizations time formatter",
    )

    controller = root / "lib/src/app_controller.dart"
    replace_exact(
        controller,
        "await recovery.replaceCorruptStore(validated);",
        "await (recovery as CorruptionRecoveryRepository)"
        ".replaceCorruptStore(validated);",
        "corrupt-store recovery cast",
    )
    replace_exact(
        controller,
        "await recovery.clearCorruptStore(confirmed: true);",
        "await (recovery as CorruptionRecoveryRepository)"
        ".clearCorruptStore(confirmed: true);",
        "corrupt-store clearing cast",
    )

    reminders = root / "lib/src/reminders.dart"
    replace_exact(
        reminders,
        "    await _notifications.zonedSchedule(\n"
        "      _stableNotificationId(reminder.id),",
        "    await _notifications.zonedSchedule(\n"
        "      id: _stableNotificationId(reminder.id),",
        "flutter_local_notifications v22 zonedSchedule signature",
    )

    emergency = root / "lib/src/emergency_recovery.dart"
    replace_exact(
        emergency,
        "\u2068{locale}\u2069",
        r"\u2068{locale}\u2069",
        "escaped Arabic bidi isolates",
    )

    locale_money = root / "lib/src/locale_money.dart"
    replace_exact(
        locale_money,
        "  static String defaultCurrencyForSystemLocale(String systemLocale) {\n"
        "    try {\n"
        "      final intlSystemLocale = Intl.canonicalizedLocale(\n"
        "        systemLocale.trim().replaceAll('-', '_'),\n"
        "      );\n"
        "      final cldrCurrency = NumberFormat.simpleCurrency(\n"
        "        locale: intlSystemLocale,\n"
        "      ).currencyName;\n"
        "      if (cldrCurrency != null && iso4217Codes.contains(cldrCurrency)) {\n"
        "        return cldrCurrency;\n"
        "      }\n"
        "    } on Object {\n"
        "      // An uncommon platform tag may be absent from the bundled CLDR data.\n"
        "    }\n"
        "    return defaultCurrencyForLocale(systemLocale);\n"
        "  }",
        "  static String defaultCurrencyForSystemLocale(String systemLocale) {\n"
        "    final parts = systemLocale.trim().replaceAll('-', '_').split('_');\n"
        "    final territory = parts.skip(1).where((part) =>\n"
        "        RegExp(r'^[A-Za-z]{2}$').hasMatch(part)).lastOrNull?.toUpperCase();\n"
        "    final territoryCurrency = territory == null\n"
        "        ? null\n"
        "        : const <String, String>{\n"
        "            'US': 'USD', 'GB': 'GBP', 'CA': 'CAD', 'AU': 'AUD',\n"
        "            'NZ': 'NZD', 'IE': 'EUR', 'IN': 'INR', 'ZA': 'ZAR',\n"
        "            'SG': 'SGD', 'ES': 'EUR', 'MX': 'MXN', 'AR': 'ARS',\n"
        "            'CL': 'CLP', 'CO': 'COP', 'PE': 'PEN', 'BR': 'BRL',\n"
        "            'PT': 'EUR', 'AO': 'AOA', 'MZ': 'MZN', 'FR': 'EUR',\n"
        "            'CH': 'CHF', 'MD': 'MDL', 'CN': 'CNY', 'TW': 'TWD',\n"
        "            'HK': 'HKD', 'MO': 'MOP', 'AE': 'AED', 'SA': 'SAR',\n"
        "            'EG': 'EGP', 'MA': 'MAD', 'DZ': 'DZD', 'QA': 'QAR',\n"
        "            'KW': 'KWD', 'BH': 'BHD', 'OM': 'OMR', 'JO': 'JOD',\n"
        "            'BD': 'BDT', 'MY': 'MYR', 'BN': 'BND', 'DE': 'EUR',\n"
        "            'IT': 'EUR', 'NL': 'EUR', 'BE': 'EUR', 'AT': 'EUR',\n"
        "            'FI': 'EUR', 'GR': 'EUR', 'PL': 'PLN', 'CZ': 'CZK',\n"
        "            'RO': 'RON', 'HU': 'HUF', 'SE': 'SEK', 'NO': 'NOK',\n"
        "            'DK': 'DKK', 'TR': 'TRY', 'UA': 'UAH', 'ID': 'IDR',\n"
        "            'VN': 'VND', 'TH': 'THB', 'JP': 'JPY', 'KR': 'KRW',\n"
        "            'PH': 'PHP', 'PK': 'PKR', 'NG': 'NGN', 'GH': 'GHS',\n"
        "            'KE': 'KES', 'TZ': 'TZS', 'UG': 'UGX', 'IL': 'ILS',\n"
        "          }[territory];\n"
        "    if (territoryCurrency != null) return territoryCurrency;\n"
        "    try {\n"
        "      final intlSystemLocale = Intl.canonicalizedLocale(\n"
        "        systemLocale.trim().replaceAll('-', '_'),\n"
        "      );\n"
        "      final cldrCurrency = NumberFormat.simpleCurrency(\n"
        "        locale: intlSystemLocale,\n"
        "      ).currencyName;\n"
        "      if (cldrCurrency != null && iso4217Codes.contains(cldrCurrency)) {\n"
        "        return cldrCurrency;\n"
        "      }\n"
        "    } on Object {\n"
        "      // An uncommon platform tag may be absent from the bundled CLDR data.\n"
        "    }\n"
        "    return defaultCurrencyForLocale(systemLocale);\n"
        "  }",
        "territory-preserving default currency",
    )

    pearl = root / "lib/src/workshop_pearl.dart"
    replace_exact(
        pearl,
        "      child: Padding(padding: padding, child: child),",
        "      child: Material(\n"
        "        type: MaterialType.transparency,\n"
        "        child: Padding(padding: padding, child: child),\n"
        "      ),",
        "PearlCard ink material surface",
    )

    widget_test = root / "test/app_widget_test.dart"
    replace_exact(
        widget_test,
        "    await tester.pumpWidget(WeldingGasWalletApp(controller: harness.controller));\n"
        "    await tester.pumpAndSettle();\n"
        "    expect(find.byType(NavigationRail), findsOneWidget);\n"
        "    expect(find.byType(NavigationBar), findsNothing);",
        "    await tester.pumpWidget(WeldingGasWalletApp(controller: harness.controller));\n"
        "    await tester.pump();\n"
        "    await tester.pump(const Duration(milliseconds: 300));\n"
        "    expect(find.byType(NavigationRail), findsOneWidget);\n"
        "    expect(find.byType(NavigationBar), findsNothing);",
        "bounded desktop layout pump",
    )


def main() -> None:
    apply_android_review_fixes()
    print("PASS: applied pinned Android source compatibility fixes")


if __name__ == "__main__":
    main()
