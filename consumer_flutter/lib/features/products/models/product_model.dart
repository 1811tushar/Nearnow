class ProductModel {
  final String id;
  final String name;
  final String description;
  final String categoryId;
  final List<String> images;
  final double price;
  final double salePrice;
  final String unit;
  final int stock;
  final double rating;
  final bool isFeatured;
  final String barcode;
  final int reviewCount;

  ProductModel({
    required this.id,
    required this.name,
    required this.description,
    required this.categoryId,
    required this.images,
    required this.price,
    required this.salePrice,
    required this.unit,
    required this.stock,
    required this.rating,
    required this.isFeatured,
    this.barcode = '',
    this.reviewCount = 0,
  });

  double get effectivePrice => salePrice > 0 ? salePrice : price;

  int get discountPercent {
    if (salePrice <= 0 || salePrice >= price || price <= 0) return 0;
    return (((price - salePrice) / price) * 100).round();
  }

  factory ProductModel.fromApi(Map<String, dynamic> json) {
    return ProductModel(
      id: json['id'].toString(),
      name: json['name'] as String? ?? '',
      description: json['description'] as String? ?? '',
      categoryId: json['categoryId']?.toString() ?? '',
      images: List<String>.from(json['images'] ?? []),
      price: (json['price'] as num?)?.toDouble() ?? 0,
      salePrice: (json['salePrice'] as num?)?.toDouble() ?? 0,
      unit: json['unit'] as String? ?? '',
      stock: json['stock'] as int? ?? 0,
      rating: (json['rating'] as num?)?.toDouble() ?? 0,
      isFeatured: json['isFeatured'] as bool? ?? false,
      barcode: json['barcode'] as String? ?? '',
      reviewCount: json['reviewCount'] as int? ?? 0,
    );
  }
}