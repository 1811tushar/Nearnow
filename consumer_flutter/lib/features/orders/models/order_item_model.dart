class OrderItemModel {
  final String productId;
  final String name;
  final String image;
  final double price;
  final String unit;
  final int quantity;

  OrderItemModel({
    required this.productId,
    required this.name,
    required this.image,
    required this.price,
    required this.unit,
    required this.quantity,
  });

  factory OrderItemModel.fromApi(Map<String, dynamic> json) {
    return OrderItemModel(
      productId: json['productId'].toString(),
      name: json['name'] as String? ?? '',
      image: json['image'] as String? ?? '',
      price: (json['price'] as num?)?.toDouble() ?? 0,
      unit: json['unit'] as String? ?? '',
      quantity: (json['quantity'] as num?)?.toInt() ?? 1,
    );
  }

  double get itemTotal => price * quantity;
}