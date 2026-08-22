import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'domain/welding_gas_wallet_core_v1_1.dart';
import 'emergency_recovery.dart';

const Map<String, String> localeNativeNames = <String, String>{
  'en': 'English',
  'es': 'Español',
  'pt': 'Português',
  'fr': 'Français',
  'de': 'Deutsch',
  'it': 'Italiano',
  'nl': 'Nederlands',
  'pl': 'Polski',
  'cs': 'Čeština',
  'ro': 'Română',
  'hu': 'Magyar',
  'sv': 'Svenska',
  'nb': 'Norsk bokmål',
  'da': 'Dansk',
  'fi': 'Suomi',
  'tr': 'Türkçe',
  'ar': 'العربية',
  'hi': 'हिन्दी',
  'bn': 'বাংলা',
  'id': 'Bahasa Indonesia',
  'vi': 'Tiếng Việt',
  'th': 'ไทย',
  'ja': '日本語',
  'ko': '한국어',
  'zh-Hans': '简体中文',
  'zh-Hant': '繁體中文',
  'uk': 'Українська',
  'el': 'Ελληνικά',
  'ms': 'Bahasa Melayu',
  'fil': 'Filipino',
};

final RegExp _placeholderPattern = RegExp(r'\{([A-Za-z][A-Za-z0-9_]*)\}');

Locale flutterLocale(String code) {
  final canonical = canonicalLocale(code);
  final parts = canonical.split('-');
  return parts.length == 2
      ? Locale.fromSubtags(languageCode: parts.first, scriptCode: parts.last)
      : Locale(canonical);
}

final List<Locale> flutterSupportedLocales =
    supportedLocales.map(flutterLocale).toList(growable: false);

String _assetForLocale(String locale) =>
    'assets/l10n/app_${canonicalLocale(locale).replaceAll('-', '_')}.arb';

Set<String> _placeholders(String value) => _placeholderPattern
    .allMatches(value)
    .map((match) => match.group(1)!)
    .toSet();

void _validateCatalog({
  required String locale,
  required Map<String, String> catalog,
  required Map<String, String> english,
}) {
  final missing = english.keys.toSet().difference(catalog.keys.toSet());
  final extra = catalog.keys.toSet().difference(english.keys.toSet());
  if (missing.isNotEmpty || extra.isNotEmpty) {
    throw FormatException(
      'Catalog key mismatch: missing=${missing.length}, extra=${extra.length}.',
    );
  }

  for (final key in english.keys) {
    final translated = catalog[key]!;
    if (translated.trim().isEmpty) {
      throw FormatException('Catalog value is empty: $locale/$key.');
    }
    final expectedPlaceholders = _placeholders(english[key]!);
    final actualPlaceholders = _placeholders(translated);
    if (!setEquals(expectedPlaceholders, actualPlaceholders)) {
      throw FormatException(
        'Catalog placeholder mismatch: $locale/$key '
        'expected=$expectedPlaceholders actual=$actualPlaceholders.',
      );
    }
  }
}

class EmergencyRecoveryStrings {
  const EmergencyRecoveryStrings({
    required this.locale,
    required this.title,
    required this.body,
    required this.retry,
  });

  final String locale;
  final String title;
  final String body;
  final String retry;

  bool get isRtl => isRtlLocale(locale);
}

EmergencyRecoveryStrings emergencyRecoveryForLocale(String requestedLocale) {
  final locale = canonicalLocale(requestedLocale);
  final catalog = emergencyRecovery[locale];
  if (catalog == null ||
      catalog.keys.toSet().difference(const <String>{
        'title',
        'body',
        'retry',
      }).isNotEmpty ||
      catalog.length != 3) {
    // A supported locale must never silently inherit another locale's copy.
    throw StateError('Emergency localization catalog is incomplete for $locale.');
  }
  String requiredValue(String key) {
    final value = catalog[key];
    if (value == null || value.trim().isEmpty) {
      throw StateError('Emergency localization value is missing: $locale/$key.');
    }
    return value;
  }

  return EmergencyRecoveryStrings(
    locale: locale,
    title: requiredValue('title'),
    body: requiredValue('body').replaceAll('{locale}', locale),
    retry: requiredValue('retry'),
  );
}

class AppStrings {
  AppStrings._(this.locale, this._messages);

  final String locale;
  final Map<String, String> _messages;

  bool get isRtl => isRtlLocale(locale);

  String call(
    String key, [
    Map<String, Object?> values = const <String, Object?>{},
  ]) {
    var result = _messages[key] ?? key;
    for (final entry in values.entries) {
      result = result.replaceAll('{${entry.key}}', entry.value.toString());
    }
    return result;
  }

  static Future<AppStrings> load(String requestedLocale) async {
    final locale = canonicalLocale(requestedLocale);
    final Map<String, String> english;
    try {
      english = await _readCatalog('assets/l10n/app_en.arb');
      _validateCatalog(locale: 'en', catalog: english, english: english);
    } on Object catch (error) {
      throw LocalizationCatalogException('en', error);
    }
    if (locale == 'en') return AppStrings._('en', english);
    try {
      final translated = await _readCatalog(_assetForLocale(locale));
      _validateCatalog(locale: locale, catalog: translated, english: english);
      return AppStrings._(locale, translated);
    } on Object catch (error) {
      throw LocalizationCatalogException(locale, error);
    }
  }

  static Future<Map<String, String>> _readCatalog(String asset) async {
    final source = await rootBundle.loadString(asset);
    final Object? decoded = jsonDecode(source);
    if (decoded is! Map<Object?, Object?>) {
      throw const FormatException('Localization catalog must be an object.');
    }
    final result = <String, String>{};
    for (final entry in decoded.entries) {
      final key = entry.key.toString();
      if (!key.startsWith('@') && entry.value is String) {
        result[key] = entry.value as String;
      }
    }
    return Map<String, String>.unmodifiable(result);
  }
}

class LocalizationCatalogException implements Exception {
  const LocalizationCatalogException(this.locale, this.cause);

  final String locale;
  final Object cause;

  @override
  String toString() =>
      'Localization catalog for $locale could not be loaded: $cause';
}
