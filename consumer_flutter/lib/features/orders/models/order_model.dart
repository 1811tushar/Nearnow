import '../../address/models/address_model.dart';
import 'order_item_model.dart';

class OrderModel {
  final String id;
  final List<OrderItemModel> items;
  final double totalAmount;
  final String status;
  final bool isCancellable;
  final String paymentMethod;
  final AddressModel deliveryAddress;
  final DateTime createdAt;

  OrderModel({
    required this.id,
    required this.items,
    required this.totalAmount,
    required this.status,
    required this.isCancellable,
    required this.paymentMethod,
    required this.deliveryAddress,
    required this.createdAt,
  });

  // Backend's OrderResponseDTO. Status arrives as an uppercase Java enum
  // name (PLACED, PACKED, ...) — lowercased here so existing UI helpers
  // (_statusLabel, _statusColor, _currentStepIndex) that switch on
  // lowercase strings keep working unchanged. isCancellable now comes
  // straight from the backend instead of being recomputed client-side —
  // fixes the exact case-mismatch bug a case-string comparison would
  // otherwise risk.
  factory OrderModel.fromApi(Map<String, dynamic> json) {
    final addr = json['deliveryAddress'] as Map<String, dynamic>;
    return OrderModel(
      id: json['id'].toString(),
      items: (json['items'] as List)
          .map((j) => OrderItemModel.fromApi(j as Map<String, dynamic>))
          .toList(),
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
      status: (json['status'] as String).toLowerCase(),
      isCancellable: json['isCancellable'] as bool? ?? false,
      paymentMethod: json['paymentMethod'] as String? ?? '',
      deliveryAddress: AddressModel.fromMap('embedded', {
        'label': addr['label'],
        'fullName': addr['fullName'],
        'phone': addr['phone'],
        'addressLine': addr['addressLine'],
        'city': addr['city'],
        'pincode': addr['pincode'],
        'latitude': addr['latitude'],
        'longitude': addr['longitude'],
        'isDefault': false,
      }),
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}