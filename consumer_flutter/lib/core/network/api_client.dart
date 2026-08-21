
import 'dart:convert';
import 'package:flutter/foundation.dart' show kIsWeb, defaultTargetPlatform, TargetPlatform;
import 'package:http/http.dart' as http;

import 'api_exception.dart';

/// Single shared wrapper around every HTTP call this app makes. Every
/// feature's *_service.dart builds on top of THIS — none of them
/// re-implement headers, token-attachment, or response-unwrapping.
class ApiClient {
  ApiClient._internal();
  static final ApiClient instance = ApiClient._internal();

  /// ⚠️ MUST match whatever's actually running the app:
  ///   - Android emulator -> 10.0.2.2 (emulator's alias for the host
  ///     machine's own localhost — NOT the same as "localhost" from
  ///     inside the emulator, which would mean the emulator itself)
  ///   - iOS simulator / Flutter web -> localhost works directly
  ///   - Physical device -> the host PC's actual LAN IP (e.g.
  ///     192.168.x.x), since a phone isn't "inside" the dev machine
  static String get baseUrl {
    const configured = String.fromEnvironment('NEARNOW_API_BASE_URL');
    if (configured.isNotEmpty) return configured;
    if (kIsWeb) return 'http://localhost:8080/api';
    if (defaultTargetPlatform == TargetPlatform.android) return 'http://10.0.2.2:8080/api';
    return 'http://localhost:8080/api';
  }

  String? _token;

  void setToken(String? token) => _token = token;

  Map<String, String> get _headers {
    final headers = {'Content-Type': 'application/json'};
    if (_token != null) headers['Authorization'] = 'Bearer $_token';
    return headers;
  }

  Future<dynamic> get(String path) async {
    final res = await http.get(Uri.parse('$baseUrl$path'), headers: _headers);
    return _handle(res);
  }

  Future<dynamic> post(String path, {Object? body}) async {
    final res = await http.post(
      Uri.parse('$baseUrl$path'),
      headers: _headers,
      body: body != null ? jsonEncode(body) : null,
    );
    return _handle(res);
  }

  Future<dynamic> put(String path, {Object? body}) async {
    final res = await http.put(
      Uri.parse('$baseUrl$path'),
      headers: _headers,
      body: body != null ? jsonEncode(body) : null,
    );
    return _handle(res);
  }
    Future<dynamic> patch(String path, {Object? body}) async {
    final res = await http.patch(
      Uri.parse('$baseUrl$path'),
      headers: _headers,
      body: body != null ? jsonEncode(body) : null,
    );
    return _handle(res);
  }

  Future<dynamic> delete(String path) async {
    final res = await http.delete(Uri.parse('$baseUrl$path'), headers: _headers);
    return _handle(res);
  }

  /// Unwraps the backend's universal ApiResponse<T> envelope:
  /// {success, data, message, timestamp}. Success -> returns `data`
  /// as-is (caller's *_service.dart does the actual model-parsing).
  /// Failure -> throws ApiException with the backend's own message,
  /// so a Cart over-stock rejection or an Order validation-error
  /// reaches the UI as real, specific text — not a generic failure.
  dynamic _handle(http.Response res) {
    Map<String, dynamic>? decoded;
    try {
      if (res.body.isNotEmpty) {
        decoded = jsonDecode(res.body) as Map<String, dynamic>;
      }
    } catch (_) {
      // Non-JSON body (rare — e.g. a raw 500 from a proxy in front of
      // the backend, not from GlobalExceptionHandler itself).
    }

    final success = decoded?['success'] as bool? ??
        (res.statusCode >= 200 && res.statusCode < 300);

    if (success) {
      return decoded?['data'];
    }

    final message = decoded?['message'] as String? ??
        'Something went wrong. Please try again.';
    throw ApiException(statusCode: res.statusCode, message: message);
  }
}