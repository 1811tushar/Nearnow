import 'dart:ui' show PlatformDispatcher;
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../l10n/app_localizations.dart';

/// Holds the app's current language and persists the choice locally so the
/// selected language is remembered the next time the app is opened.
class LocaleProvider extends ChangeNotifier {
  static const _prefsKey = 'app_locale_code';
  static const _fallback = Locale('en');

  Locale _locale = _fallback;
  Locale get locale => _locale;

  bool _isSupported(String? languageCode) {
    if (languageCode == null) return false;
    return AppLocalizations.supportedLocales
        .any((l) => l.languageCode == languageCode);
  }

  /// Call this once, early in app startup, to load any previously saved
  /// language choice before the first frame is shown.
  ///
  /// Resolution order:
  /// 1. A previously saved, still-supported language choice.
  /// 2. The device's current language, if it's one we support.
  /// 3. English, as a safe default.
  Future<void> loadSavedLocale() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final savedCode = prefs.getString(_prefsKey);

      if (_isSupported(savedCode)) {
        _locale = Locale(savedCode!);
        notifyListeners();
        return;
      }

      // No valid saved preference — try the device's own language before
      // giving up and defaulting to English.
      final deviceCode = PlatformDispatcher.instance.locale.languageCode;
      if (_isSupported(deviceCode)) {
        _locale = Locale(deviceCode);
        notifyListeners();
      }
    } catch (_) {
      // If reading local storage fails for any reason (e.g. platform channel
      // not ready, corrupted prefs), fail safe to the English default rather
      // than crashing app startup.
      _locale = _fallback;
    }
  }

  Future<void> setLocale(Locale locale) async {
    if (_locale == locale) return;
    _locale = locale;
    notifyListeners();

    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_prefsKey, locale.languageCode);
    } catch (_) {
      // The UI has already switched language for this session; a failed
      // persist just means it won't be remembered on next launch. Not
      // fatal, so we swallow it here rather than surfacing an error for
      // something this low-stakes.
    }
  }
}
