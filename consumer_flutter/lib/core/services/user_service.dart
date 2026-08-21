import '../network/api_client.dart';
import '../models/user_model.dart';

class UserService {
  final ApiClient _client;

  UserService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<UserModel> getCurrentUser() async {
    final data = await _client.get('/auth/me');
    return UserModel.fromApi(data as Map<String, dynamic>);
  }

  Future<UserModel> updateProfile({
    required String fullName,
    required String phone,
    String? photoUrl,
  }) async {
    final data = await _client.put('/auth/profile', body: {
      'fullName': fullName,
      'phone': phone,
      if (photoUrl != null) 'photoUrl': photoUrl,
    });
    return UserModel.fromApi(data as Map<String, dynamic>);
  }

  Future<UserModel> updateNotificationPreference(bool enabled) async {
    final data = await _client.patch('/auth/notifications', body: {
      'enabled': enabled,
    });
    return UserModel.fromApi(data as Map<String, dynamic>);
  }
}