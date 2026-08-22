from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from apply_android_review_fixes import apply_android_review_fixes, replace_exact


class AndroidReviewFixesTest(unittest.TestCase):
    def _fixture(self, root: Path) -> None:
        source = root / "lib/src"
        source.mkdir(parents=True)
        (source / "app.dart").write_text(
            "import 'package:intl/intl.dart';\n"
            "      CylinderEventType.relationshipChanged => c.t('relationshipChanged'),\n"
            "      CylinderEventType.returned => c.t('eventReturned'),\n"
            "material.formatTime(TimeOfDay.fromDateTime(local))\n",
            encoding="utf-8",
        )
        (source / "app_controller.dart").write_text(
            "await recovery.replaceCorruptStore(validated);\n"
            "await recovery.clearCorruptStore(confirmed: true);\n",
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

    def test_full_fix_set_is_idempotent(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            self._fixture(root)

            apply_android_review_fixes(root)
            first = {
                path.name: path.read_text(encoding="utf-8")
                for path in (root / "lib/src").iterdir()
            }
            apply_android_review_fixes(root)
            second = {
                path.name: path.read_text(encoding="utf-8")
                for path in (root / "lib/src").iterdir()
            }

            self.assertEqual(first, second)
            self.assertIn("show DateFormat", first["app.dart"])
            self.assertIn("CylinderEventType.note || CylinderEventType.photoAdded", first["app.dart"])
            self.assertIn("formatTimeOfDay", first["app.dart"])
            self.assertIn("as CorruptionRecoveryRepository", first["app_controller.dart"])
            self.assertIn("id: _stableNotificationId", first["reminders.dart"])
            self.assertIn(r"\u2068{locale}\u2069", first["emergency_recovery.dart"])
            self.assertNotIn("\u2068{locale}\u2069", first["emergency_recovery.dart"].replace(r"\u2068{locale}\u2069", ""))

    def test_replace_exact_fails_closed_on_source_drift(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "source.dart"
            path.write_text("unexpected source", encoding="utf-8")
            with self.assertRaises(SystemExit):
                replace_exact(path, "reviewed old", "reviewed new", "fixture")


if __name__ == "__main__":
    unittest.main()
