
import 'dart:math' as math;

/// Real distance-based delivery estimate — replaces the old hardcoded
/// "13 mins" label. MVP-simplified per the original roadmap: a single
/// fixed dark-store coordinate, not real multi-warehouse routing.
class DeliveryEstimate {
  DeliveryEstimate._();

  // Single fixed dark store — Connaught Place, New Delhi. Swap this for
  // a real warehouse coordinate, or extend to nearest-of-many later.
  static const double _storeLat = 28.6315;
  static const double _storeLng = 77.2167;

  static const int _baseMinutes = 8;
  static const double _minutesPerKm = 1.5;
  static const int _minEstimate = 8;
  static const int _maxEstimate = 45;

  static double _toRadians(double degrees) => degrees * (math.pi / 180);

  static double _haversineKm(
      double lat1, double lng1, double lat2, double lng2) {
    const earthRadiusKm = 6371.0;
    final dLat = _toRadians(lat2 - lat1);
    final dLng = _toRadians(lng2 - lng1);

    final a = math.sin(dLat / 2) * math.sin(dLat / 2) +
        math.cos(_toRadians(lat1)) *
            math.cos(_toRadians(lat2)) *
            math.sin(dLng / 2) *
            math.sin(dLng / 2);
    final c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a));

    return earthRadiusKm * c;
  }

  static String estimateFor({double? latitude, double? longitude}) {
    final hasRealLocation =
        latitude != null && longitude != null && !(latitude == 0 && longitude == 0);

    if (!hasRealLocation) {
      return "10-15 mins";
    }

    final distanceKm = _haversineKm(_storeLat, _storeLng, latitude, longitude);
    final minutes =
        (_baseMinutes + (distanceKm * _minutesPerKm)).round().clamp(
              _minEstimate,
              _maxEstimate,
            );

    return "$minutes mins";
  }
}