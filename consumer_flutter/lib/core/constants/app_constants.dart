class AppConstants {
  AppConstants._();

  static const String appName = "NearNow";
  static const String companyName = "NearNow";
  static const String tagline = "Delivered near, delivered now";
  static const String version = "1.0.0";

  static const String currencySymbol = "₹";
  static const String currencyCode = "INR";

  static const int minPasswordLength = 6;

  /// Flat delivery fee charged below the free-delivery threshold.
  static const double deliveryFee = 20;

  /// Order subtotal at or above which delivery is free.
  static const double freeDeliveryThreshold = 199;
}