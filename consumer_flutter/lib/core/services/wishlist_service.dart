import '../network/api_client.dart';
import '../models/wishlist_model.dart';

class WishlistService {
  final ApiClient _client;

  WishlistService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<WishlistModel> getWishlist() async {
    final data = await _client.get('/wishlist');
    return WishlistModel.fromApi(data as Map<String, dynamic>);
  }

  // Backend's add/remove are idempotent — calling add on an
  // already-wishlisted product (or remove on one that isn't there)
  // never errors, matching the old arrayUnion/arrayRemove behavior.
  Future<void> addToWishlist(String productId) async {
    await _client.post('/wishlist/add/$productId');
  }

  Future<void> removeFromWishlist(String productId) async {
    await _client.delete('/wishlist/remove/$productId');
  }
}