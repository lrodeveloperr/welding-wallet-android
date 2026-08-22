#!/usr/bin/env python3
"""Apply deterministic source fixes required by the pinned Android toolchain.

The reviewed source is shared with iOS, while Android CI pins Flutter 3.44.8 and
flutter_local_notifications 22.3.0. These edits are deliberately exact and
fail closed so SDK/API drift cannot silently change production behavior.
"""

from __future__ import annotations

from pathlib import Path


def replace_exact(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if new in text and old not in text:
        return
    if old not in text:
        raise SystemExit(f"Android source fix failed: missing {label} in {path}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def main() -> None:
    app = Path("lib/src/app.dart")
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

    controller = Path("lib/src/app_controller.dart")
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

    reminders = Path("lib/src/reminders.dart")
    replace_exact(
        reminders,
        "    await _notifications.zonedSchedule(\n"
        "      _stableNotificationId(reminder.id),",
        "    await _notifications.zonedSchedule(\n"
        "      id: _stableNotificationId(reminder.id),",
        "flutter_local_notifications v22 zonedSchedule signature",
    )

    emergency = Path("lib/src/emergency_recovery.dart")
    replace_exact(
        emergency,
        "\u2068{locale}\u2069",
        r"\u2068{locale}\u2069",
        "escaped Arabic bidi isolates",
    )

    print("PASS: applied pinned Android source compatibility fixes")


if __name__ == "__main__":
    main()
