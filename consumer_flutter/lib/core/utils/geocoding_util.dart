
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../constants/app_constants.dart';

/// Converts a typed address string into real coordinates using
/// OpenStreetMap's free Nominatim geocoding API (no API key required).
///
/// Returns null if the address couldn't be resolved (bad network, no
/// match found, etc.) — callers should save the address anyway and
/// treat this as "we don't know exactly where this is."
class GeocodingUtil {
  static Future<({double lat, double lng})?> geocode(String query) async {
    if (query.trim().isEmpty) return null;

    final uri = Uri.https(
      'nominatim.openstreetmap.org',
      '/search',
      {
        'q': query,
        'format': 'json',
        'limit': '1',
      },
    );

    try {
      // Nominatim's usage policy requires a descriptive User-Agent —
      // without one, requests can get silently rate-limited/blocked.
      final response = await http.get(
        uri,
        headers: {
          'User-Agent': '${AppConstants.appName}/${AppConstants.version} (grocery delivery app)'
        },
      ).timeout(const Duration(seconds: 8));

      if (response.statusCode != 200) return null;

      final results = jsonDecode(response.body) as List;
      if (results.isEmpty) return null;

      final first = results.first as Map<String, dynamic>;
      final lat = double.tryParse(first['lat'] as String);
      final lng = double.tryParse(first['lon'] as String);

      if (lat == null || lng == null) return null;
      return (lat: lat, lng: lng);
    } catch (_) {
      return null;
    }
  }
}