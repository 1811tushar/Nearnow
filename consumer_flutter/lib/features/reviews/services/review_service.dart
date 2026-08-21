import '../../../core/network/api_client.dart';
import '../models/review_model.dart';

/// REST implementation for the NearNow review API.
///
/// The backend derives the authenticated user from the JWT and the product
/// from the URL path. The client therefore sends only rating/comment and
/// never sends userId/productId as trusted request-body fields.
class ReviewService {
  final ApiClient _client;

  ReviewService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<void> addReview(ReviewModel review) async {
    await _client.post('/reviews/product/${review.productId}', body: {
      'rating': review.rating,
      'comment': review.comment,
    });
  }

  Future<List<ReviewModel>> getReviewsByProduct(String productId) async {
    final data = await _client.get('/reviews/product/$productId');
    return (data as List)
        .map((json) => ReviewModel.fromApi(
              productId,
              json as Map<String, dynamic>,
            ))
        .toList();
  }
}
