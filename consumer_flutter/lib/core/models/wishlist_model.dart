class WishlistModel {
  final List<String> productIds;

  WishlistModel({required this.productIds});

  /// Backend's WishlistResponseDTO is deliberately just a list of numeric
  /// product-ids ({productIds: [...]}) — full product-details are fetched
  /// separately via ProductProvider's existing batch-by-ids endpoint,
  /// same as the old Firestore version already did.
  factory WishlistModel.fromApi(Map<String, dynamic> json) {
    final ids = json['productIds'] as List;
    return WishlistModel(productIds: ids.map((id) => id.toString()).toList());
  }
}