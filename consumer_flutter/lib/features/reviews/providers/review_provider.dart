import 'package:flutter/material.dart';
import '../models/review_model.dart';
import '../services/review_service.dart';

class ReviewProvider extends ChangeNotifier {
  final ReviewService _reviewService;

  ReviewProvider({ReviewService? reviewService})
      : _reviewService = reviewService ?? ReviewService();

  List<ReviewModel> _reviews = [];
  bool _isLoading = false;
  String? _error;

  List<ReviewModel> get reviews => _reviews;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> fetchReviews(String productId) async {
    try {
      _isLoading = true;
      notifyListeners();

      _reviews = await _reviewService.getReviewsByProduct(productId);
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> submitReview(ReviewModel review) async {
    try {
      _isLoading = true;
      notifyListeners();

      await _reviewService.addReview(review);
      await fetchReviews(review.productId);

      _error = null;
      return true;
    } catch (e) {
      _error = e.toString();
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void clear() {
    _reviews = [];
    _error = null;
    _isLoading = false;
    notifyListeners();
  }
}
