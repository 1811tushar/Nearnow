import 'package:flutter/material.dart';
import '../../../core/services/wishlist_service.dart';

class WishlistProvider extends ChangeNotifier {
  final WishlistService _wishlistService;

  WishlistProvider({WishlistService? wishlistService})
      : _wishlistService = wishlistService ?? WishlistService();

  List<String> _wishlistIds = [];
  bool _isLoading = false;
  String? _error;

  List<String> get wishlistIds => _wishlistIds;
  bool get isLoading => _isLoading;
  String? get error => _error;

  bool isInWishlist(String productId) => _wishlistIds.contains(productId);

  // uid kept (unused internally) purely so every existing call-site
  // keeps compiling unchanged — the backend identifies the user from
  // the JWT, not from this value.
  Future<void> fetchWishlist(String uid) async {
    try {
      _isLoading = true;
      notifyListeners();

      final wishlist = await _wishlistService.getWishlist();
      _wishlistIds = wishlist.productIds;
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> toggleWishlist(String uid, String productId) async {
    final wasInWishlist = isInWishlist(productId);

    if (wasInWishlist) {
      _wishlistIds.remove(productId);
    } else {
      _wishlistIds.add(productId);
    }
    notifyListeners();

    try {
      if (wasInWishlist) {
        await _wishlistService.removeFromWishlist(productId);
      } else {
        await _wishlistService.addToWishlist(productId);
      }
      _error = null;
    } catch (e) {
      if (wasInWishlist) {
        _wishlistIds.add(productId);
      } else {
        _wishlistIds.remove(productId);
      }
      _error = e.toString();
      notifyListeners();
    }
  }

  void clear() {
    _wishlistIds = [];
    _error = null;
    _isLoading = false;
    notifyListeners();
  }
}