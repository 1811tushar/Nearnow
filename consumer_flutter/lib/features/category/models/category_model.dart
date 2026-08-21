class CategoryModel {
  final String id;
  final String name;
  final String imageUrl;
  final String? parentCategoryId;
  final int sortOrder;

  CategoryModel({
    required this.id,
    required this.name,
    required this.imageUrl,
    this.parentCategoryId,
    required this.sortOrder,
  });

  factory CategoryModel.fromApi(Map<String, dynamic> json) {
    return CategoryModel(
      id: json['id'].toString(),
      name: json['name'] as String? ?? '',
      imageUrl: json['imageUrl'] as String? ?? '',
      parentCategoryId: json['parentCategoryId']?.toString(),
      sortOrder: json['sortOrder'] as int? ?? 0,
    );
  }

  CategoryModel copyWith({
    String? name,
    String? imageUrl,
    String? parentCategoryId,
    int? sortOrder,
  }) {
    return CategoryModel(
      id: id,
      name: name ?? this.name,
      imageUrl: imageUrl ?? this.imageUrl,
      parentCategoryId: parentCategoryId ?? this.parentCategoryId,
      sortOrder: sortOrder ?? this.sortOrder,
    );
  }
}