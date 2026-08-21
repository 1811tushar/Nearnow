import 'package:flutter/foundation.dart';
import '../../../core/models/user_model.dart';
import '../../../core/services/user_service.dart';

class UserProvider extends ChangeNotifier {
  final UserService _userService;

  UserProvider({UserService? userService})
      : _userService = userService ?? UserService();

  UserModel? _user;
  bool _isLoading = false;
  String? _error;

  UserModel? get user => _user;
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Fetches the signed-in user's profile from the backend
  /// (GET /api/auth/me) — the token already attached by ApiClient
  /// identifies WHO "current user" means, so no uid parameter needed
  /// anymore (unlike the old Firestore-doc-by-uid lookup).
  Future<void> loadCurrentUser() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _user = await _userService.getCurrentUser();
    } catch (e) {
      _error = e.toString();
      rethrow; // AuthProvider.tryAutoLogin needs to know this failed
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Calls PUT /api/auth/profile and updates local state with the
  /// backend's saved copy on success.
  Future<bool> updateProfile({
    required String uid,
    String? fullName,
    String? phone,
    String? photoUrl,
    String? displayName,
    String? phoneNumber,
  }) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _user = await _userService.updateProfile(
        fullName: fullName ?? _user?.fullName ?? '',
        phone: phone ?? _user?.phone ?? '',
        photoUrl: photoUrl,
      );
      return true;
    } catch (e) {
      _error = e.toString();
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
  Future<void> toggleNotifications(bool enable) async {
    _error = null;
    try {
      _user = await _userService.updateNotificationPreference(enable);
    } catch (e) {
      _error = e.toString();
    } finally {
      notifyListeners();
    }
  }
  

  void clear() {
    _user = null;
    _error = null;
    _isLoading = false;
    notifyListeners();
  }
}