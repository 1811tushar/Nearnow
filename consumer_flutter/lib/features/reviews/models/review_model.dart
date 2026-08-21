class ReviewModel {
  final String id;
  final String productId;
  final String userId;
  final String userName;
  final double rating;
  final String comment;
  final DateTime createdAt;

  ReviewModel({
    required this.id,
    required this.productId,
    required this.userId,
    required this.userName,
    required this.rating,
    required this.comment,
    required this.createdAt,
  });

  /// Maps the backend ReviewResponseDTO. The API response does not repeat
  /// productId because it is already part of the request URL, so the service
  /// supplies it explicitly.
  factory ReviewModel.fromApi(
    String productId,
    Map<String, dynamic> json,
  ) {
    return ReviewModel(
      id: json['id'].toString(),
      productId: productId,
      userId: json['userId'].toString(),
      userName: json['userName'] as String? ?? '',
      rating: (json['rating'] as num?)?.toDouble() ?? 0,
      comment: json['comment'] as String? ?? '',
      createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? '') ??
          DateTime.now(),
    );
  }
}
