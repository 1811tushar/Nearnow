
import '../network/api_client.dart';

/// What the backend's AuthResponseDTO looks like — token + minimal
/// user-info in one round-trip, from both /register and /login.
class AuthResult {
  final String token;
  final int id;
  final String email;
  final String fullName;
  final String role;

  AuthResult({
    required this.token,
    required this.id,
    required this.email,
    required this.fullName,
    required this.role,
  });

  factory AuthResult.fromJson(Map<String, dynamic> json) {
    return AuthResult(
      token: json['token'] as String,
      id: json['id'] as int,
      email: json['email'] as String,
      fullName: json['fullName'] as String? ?? '',
      role: json['role'] as String? ?? 'user',
    );
  }
}

class AuthApiService {
  final ApiClient _client;

  AuthApiService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<AuthResult> register({
    required String email,
    required String password,
    required String fullName,
  }) async {
    final data = await _client.post('/auth/register', body: {
      'email': email,
      'password': password,
      'fullName': fullName,
    });
    return AuthResult.fromJson(data as Map<String, dynamic>);
  }

  Future<AuthResult> login({
    required String email,
    required String password,
  }) async {
    final data = await _client.post('/auth/login', body: {
      'email': email,
      'password': password,
    });
    return AuthResult.fromJson(data as Map<String, dynamic>);
  }

  // Both of these hit permitAll backend endpoints (no token needed —
  // the whole point is the caller is locked out). Neither returns an
  // AuthResult: forgot-password never logs anyone in (it just triggers
  // an email), and reset-password intentionally logs the user out
  // everywhere (see backend's authVersion bump), so returning a fresh
  // token here would be misleading.
  Future<void> forgotPassword({required String email}) {
    return _client.post('/auth/forgot-password', body: {'email': email});
  }

  Future<void> resetPassword({
    required String email,
    required String otp,
    required String newPassword,
  }) {
    return _client.post('/auth/reset-password', body: {
      'email': email,
      'otp': otp,
      'newPassword': newPassword,
    });
  }
}