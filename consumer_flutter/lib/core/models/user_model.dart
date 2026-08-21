
class UserModel {
  final String uid;
  final String email;
  final String fullName;
  final String phone;
  final String photoUrl;
  final String role;
  final bool isEmailVerified;
  final bool notificationsEnabled;

  const UserModel({
    required this.uid,
    required this.email,
    this.fullName = '',
    this.phone = '',
    this.photoUrl = '',
    this.role = 'user',
    this.isEmailVerified = false,
    this.notificationsEnabled = true,
  });

  // Backward-compatibility getters — kept so edit_profile_page.dart's
  // existing fallback expressions (user?.fullName ?? user?.displayName)
  // keep compiling unchanged.
  String get displayName => fullName;
  String get phoneNumber => phone;

  /// Matches backend's UserResponseDTO exactly (GET /api/auth/me):
  /// id, email, fullName, phone, photoUrl, role, isEmailVerified,
  /// notificationsEnabled.
  factory UserModel.fromApi(Map<String, dynamic> json) {
    return UserModel(
      uid: json['id'].toString(),
      email: json['email'] as String? ?? '',
      fullName: json['fullName'] as String? ?? '',
      phone: json['phone'] as String? ?? '',
      photoUrl: json['photoUrl'] as String? ?? '',
      role: json['role'] as String? ?? 'user',
      isEmailVerified: json['isEmailVerified'] as bool? ?? false,
      notificationsEnabled: json['notificationsEnabled'] as bool? ?? true,
    );
  }

  UserModel copyWith({
    String? uid,
    String? email,
    String? fullName,
    String? phone,
    String? photoUrl,
    String? role,
    bool? isEmailVerified,
    bool? notificationsEnabled,
  }) {
    return UserModel(
      uid: uid ?? this.uid,
      email: email ?? this.email,
      fullName: fullName ?? this.fullName,
      phone: phone ?? this.phone,
      photoUrl: photoUrl ?? this.photoUrl,
      role: role ?? this.role,
      isEmailVerified: isEmailVerified ?? this.isEmailVerified,
      notificationsEnabled: notificationsEnabled ?? this.notificationsEnabled,
    );
  }
}