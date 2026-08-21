class CartItemModel {
  final String id;
  final String productId;
  final String name;
  final String image;
  final double price;
  final String unit;
  final int quantity;

  CartItemModel({
    required this.id,
    required this.productId,
    required this.name,
    required this.image,
    required this.price,
    required this.unit,
    required this.quantity,
  });

  // Backend's CartItemResponseDTO: `id` here is the cart-line's OWN id
  // (auto-generated, distinct from productId) — different from the old
  // Firestore version, which reused productId as the line's doc-id.
  // Every caller already treats `.id` as an opaque identifier, so this
  // doesn't require any page-level changes.
  factory CartItemModel.fromApi(Map<String, dynamic> json) {
    return CartItemModel(
      id: json['id'].toString(),
      productId: json['productId'].toString(),
      name: json['name'] as String? ?? '',
      image: json['image'] as String? ?? '',
      price: (json['price'] as num?)?.toDouble() ?? 0,
      unit: json['unit'] as String? ?? '',
      quantity: json['quantity'] as int? ?? 1,
    );
  }

  double get itemTotal => price * quantity;
}