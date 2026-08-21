import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/services/token_storage.dart';
import '../data/auth_repository.dart';
import '../../profile/providers/user_provider.dart';

/// Backward-compatibility shim: many existing pages (cart, wishlist,
/// address, orders, product-card) call `authProvider.user?.uid` — a
/// pattern from when `.user` was a Firebase User object. Rather than
/// touch every one of those call-sites, `.user` now returns this tiny
/// object with just the one field they actually ever used.
class AuthUser {
  final String uid;
  const AuthUser(this.uid);
}

class AuthProvider extends ChangeNotifier {
  final AuthRepository _repository;
  final TokenStorage _tokenStorage;

  AuthProvider({AuthRepository? repository, TokenStorage? tokenStorage})
      : _repository = repository ?? AuthRepository(),
        _tokenStorage = tokenStorage ?? TokenStorage();

  bool _isLoggedIn = false;
  bool _isLoading = false;
  bool _isInitializing = true;
  String? _error;
  AuthUser? _authUser;

  bool get isLoggedIn => _isLoggedIn;
  bool get isLoading => _isLoading;
  bool get isInitializing => _isInitializing;
  String? get error => _error;
  AuthUser? get user => _authUser;

  Future<void> tryAutoLogin(UserProvider userProvider) async {
    final savedToken = await _tokenStorage.readToken();
    if (savedToken == null) {
      _isInitializing = false;
      notifyListeners();
      return;
    }

    ApiClient.instance.setToken(savedToken);
    try {
      await userProvider.loadCurrentUser();
      _authUser = AuthUser(userProvider.user!.uid);
      _isLoggedIn = true;
    } catch (e) {
      if (e is ApiException && e.statusCode == 401) {
        await _tokenStorage.clearToken();
        ApiClient.instance.setToken(null);
        _isLoggedIn = false;
      } else {
        // Keep the token on transient network/5xx failures. The next
        // startup can retry instead of unexpectedly logging the user out.
        _isLoggedIn = false;
      }
    } finally {
      _isInitializing = false;
      notifyListeners();
    }
  }

  String _mapAuthError(Object e) {
    if (e is ApiException) {
      if (e.statusCode == 401) return 'invalid-credential';
      if (e.statusCode == 409) return 'email-already-in-use';
      if (e.statusCode == 400) return 'invalid-email';
      return 'network-request-failed';
    }
    return 'unknown-error';
  }

  Future<bool> register(
    String email,
    String password,
    UserProvider userProvider, {
    required String fullName,
  }) async {
    try {
      _isLoading = true;
      _error = null;
      notifyListeners();

      final result = await _repository.register(email, password, fullName);
      ApiClient.instance.setToken(result.token);
      await _tokenStorage.saveToken(result.token);
      await userProvider.loadCurrentUser();

      _authUser = AuthUser(result.id.toString());
      _isLoggedIn = true;
      return true;
    } catch (e) {
      _error = _mapAuthError(e);
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> login(
    String email,
    String password,
    UserProvider userProvider,
  ) async {
    try {
      _isLoading = true;
      _error = null;
      notifyListeners();

      final result = await _repository.login(email, password);
      ApiClient.instance.setToken(result.token);
      await _tokenStorage.saveToken(result.token);
      await userProvider.loadCurrentUser();

      _authUser = AuthUser(result.id.toString());
      _isLoggedIn = true;
      return true;
    } catch (e) {
      _error = _mapAuthError(e);
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> logout(UserProvider userProvider) async {
    await _tokenStorage.clearToken();
    ApiClient.instance.setToken(null);
    userProvider.clear();
    _authUser = null;
    _isLoggedIn = false;
    notifyListeners();
  }
}