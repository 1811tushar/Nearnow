
/// Thrown whenever the backend responds with `success: false`, or the
/// HTTP call itself fails (network error, timeout, unreachable host).
/// `message` is already the backend's own human-readable text (from
/// GlobalExceptionHandler) — UI code can show it directly, no need to
/// re-map error codes the way Firebase's error-codes used to require.
class ApiException implements Exception {
  final int statusCode;
  final String message;

  ApiException({required this.statusCode, required this.message});

  @override
  String toString() => message;
}