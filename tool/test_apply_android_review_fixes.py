from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from apply_android_review_fixes import apply_android_review_fixes, replace_exact


class AndroidReviewFixesTest(unittest.TestCase):
    def _fixture(self, root: Path) -> None:
        source = root / "lib/src"
        source.mkdir(parents=True)
        tests = root / "test"
        tests.mkdir(parents=True)
        (source / "app.dart").write_text(
            "import 'package:intl/intl.dart';\n"
            "      CylinderEventType.relationshipChanged => c.t('relationshipChanged'),\n"
            "      CylinderEventType.returned => c.t('eventReturned'),\n"
            "material.formatTime(TimeOfDay.fromDateTime(local))\n"
            "if (mounted && c.errorMessage == null) Navigator.pop(context);\n"
            "class RecordActionSheet extends StatefulWidget {}\n"
            "State<RecordActionSheet> createState() => _RecordActionSheetState();\n"
            "class _RecordActionSheetState extends State<RecordActionSheet> {}\n",
            encoding="utf-8",
        )
        (source / "app_controller.dart").write_text(
            "await recovery.replaceCorruptStore(validated);\n"
            "await recovery.clearCorruptStore(confirmed: true);\n"
            "    final subscription = _storeUpdateSubscription;\n"
            "    if (subscription != null) unawaited(subscription.cancel());\n",
            encoding="utf-8",
        )
        (source / "reminders.dart").write_text(
            "    await _notifications.zonedSchedule(\n"
            "      _stableNotificationId(reminder.id),\n",
            encoding="utf-8",
        )
        (source / "emergency_recovery.dart").write_text(
            "body: \"locale \u2068{locale}\u2069\"\n",
            encoding="utf-8",
        )
        (source / "locale_money.dart").write_text(
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
            "  }\n",
            encoding="utf-8",
        )
        (source / "workshop_pearl.dart").write_text(
            "      child: Padding(padding: padding, child: child),\n",
            encoding="utf-8",
        )
        (tests / "app_widget_test.dart").write_text(
            "    await tester.pumpWidget(WeldingGasWalletApp(controller: harness.controller));\n"
            "    await tester.pumpAndSettle();\n"
            "    expect(find.byType(NavigationRail), findsOneWidget);\n"
            "    expect(find.byType(NavigationBar), findsNothing);\n",
            encoding="utf-8",
        )

    def _snapshot(self, root: Path) -> dict[str, str]:
        paths = [
            *(root / "lib/src").iterdir(),
            root / "test/app_widget_test.dart",
        ]
        return {
            str(path.relative_to(root)): path.read_text(encoding="utf-8")
            for path in paths
        }

    def test_full_fix_set_is_idempotent(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self._fixture(root)

            apply_android_review_fixes(root)
            first = self._snapshot(root)
            apply_android_review_fixes(root)
            second = self._snapshot(root)

            self.assertEqual(first, second)
            self.assertIn("show DateFormat", first["lib/src/app.dart"])
            self.assertIn(
                "CylinderEventType.note || CylinderEventType.photoAdded",
                first["lib/src/app.dart"],
            )
            self.assertIn("formatTimeOfDay", first["lib/src/app.dart"])
            self.assertIn("context.mounted", first["lib/src/app.dart"])
            self.assertIn("class _RecordActionSheet", first["lib/src/app.dart"])
            self.assertNotIn("class RecordActionSheet", first["lib/src/app.dart"])
            self.assertIn(
                "as CorruptionRecoveryRepository",
                first["lib/src/app_controller.dart"],
            )
            self.assertIn(
                "unawaited(_storeUpdateSubscription?.cancel())",
                first["lib/src/app_controller.dart"],
            )
            self.assertIn(
                "_storeUpdateSubscription = null",
                first["lib/src/app_controller.dart"],
            )
            self.assertIn("id: _stableNotificationId", first["lib/src/reminders.dart"])
            self.assertIn(
                r"\u2068{locale}\u2069",
                first["lib/src/emergency_recovery.dart"],
            )
            self.assertIn("'AR': 'ARS'", first["lib/src/locale_money.dart"])
            self.assertEqual(
                first["lib/src/workshop_pearl.dart"].count("MaterialType.transparency"),
                1,
            )
            self.assertIn(
                "Duration(milliseconds: 300)",
                first["test/app_widget_test.dart"],
            )

    def test_replace_exact_handles_old_fragment_nested_inside_new_fragment(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "source.dart"
            old = "child: Padding(padding: padding, child: child),"
            new = (
                "child: Material(\n"
                "  type: MaterialType.transparency,\n"
                "  child: Padding(padding: padding, child: child),\n"
                "),"
            )
            path.write_text(old, encoding="utf-8")

            replace_exact(path, old, new, "nested replacement")
            first = path.read_text(encoding="utf-8")
            replace_exact(path, old, new, "nested replacement")
            second = path.read_text(encoding="utf-8")

            self.assertEqual(first, second)
            self.assertEqual(second.count("MaterialType.transparency"), 1)

    def test_replace_exact_fails_closed_on_source_drift(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "source.dart"
            path.write_text("unexpected source", encoding="utf-8")
            with self.assertRaises(SystemExit):
                replace_exact(path, "reviewed old", "reviewed new", "fixture")


if __name__ == "__main__":
    unittest.main()
