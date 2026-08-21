import '../../../core/services/auth_api_service.dart';

class AuthRepository {
  final AuthApiService _service;

  AuthRepository({AuthApiService? service})
      : _service = service ?? AuthApiService();

  Future<AuthResult> login(String email, String password) {
    return _service.login(email: email, password: password);
  }

  Future<AuthResult> register(String email, String password, String fullName) {
    return _service.register(email: email, password: password, fullName: fullName);
  }

  Future<void> forgotPassword(String email) {
    return _service.forgotPassword(email: email);
  }

  Future<void> resetPassword(String email, String otp, String newPassword) {
    return _service.resetPassword(email: email, otp: otp, newPassword: newPassword);
  }
}