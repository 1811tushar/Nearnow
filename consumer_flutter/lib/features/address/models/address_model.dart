class AddressModel {
  final String id;
  final String label;
  final String fullName;
  final String phone;
  final String addressLine;
  final String city;
  final String pincode;
  final double latitude;
  final double longitude;
  final bool isDefault;

  AddressModel({
    required this.id,
    required this.label,
    required this.fullName,
    required this.phone,
    required this.addressLine,
    required this.city,
    required this.pincode,
    required this.latitude,
    required this.longitude,
    required this.isDefault,
  });

  // Kept for order_model.dart, which embeds a delivery-address snapshot
  // inside each order using this generic Map shape — not Firestore-
  // specific, so nothing here needs to change when Orders gets wired
  // in its own turn (Stage 5).
  factory AddressModel.fromMap(String id, Map<String, dynamic> map) {
    return AddressModel(
      id: id,
      label: map['label'] ?? '',
      fullName: map['fullName'] ?? '',
      phone: map['phone'] ?? '',
      addressLine: map['addressLine'] ?? '',
      city: map['city'] ?? '',
      pincode: map['pincode'] ?? '',
      latitude: (map['latitude'] ?? 0).toDouble(),
      longitude: (map['longitude'] ?? 0).toDouble(),
      isDefault: map['isDefault'] ?? false,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'label': label,
      'fullName': fullName,
      'phone': phone,
      'addressLine': addressLine,
      'city': city,
      'pincode': pincode,
      'latitude': latitude,
      'longitude': longitude,
      'isDefault': isDefault,
    };
  }

  // Backend's AddressResponseDTO shape (GET/POST/PUT /api/addresses) —
  // the only real difference from fromMap is that `id` arrives INSIDE
  // the JSON here (backend's numeric id), rather than passed separately
  // the way Firestore's doc.id used to be.
  factory AddressModel.fromApi(Map<String, dynamic> json) {
    return AddressModel(
      id: json['id'].toString(),
      label: json['label'] as String? ?? '',
      fullName: json['fullName'] as String? ?? '',
      phone: json['phone'] as String? ?? '',
      addressLine: json['addressLine'] as String? ?? '',
      city: json['city'] as String? ?? '',
      pincode: json['pincode'] as String? ?? '',
      latitude: (json['latitude'] as num?)?.toDouble() ?? 0,
      longitude: (json['longitude'] as num?)?.toDouble() ?? 0,
      isDefault: json['isDefault'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toApi() {
    return {
      'label': label,
      'fullName': fullName,
      'phone': phone,
      'addressLine': addressLine,
      'city': city,
      'pincode': pincode,
      'latitude': latitude,
      'longitude': longitude,
      'isDefault': isDefault,
    };
  }

  AddressModel copyWith({
    String? label,
    String? fullName,
    String? phone,
    String? addressLine,
    String? city,
    String? pincode,
    double? latitude,
    double? longitude,
    bool? isDefault,
  }) {
    return AddressModel(
      id: id,
      label: label ?? this.label,
      fullName: fullName ?? this.fullName,
      phone: phone ?? this.phone,
      addressLine: addressLine ?? this.addressLine,
      city: city ?? this.city,
      pincode: pincode ?? this.pincode,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      isDefault: isDefault ?? this.isDefault,
    );
  }
}