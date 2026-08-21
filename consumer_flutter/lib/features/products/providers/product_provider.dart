import 'package:flutter/material.dart';
import '../models/product_model.dart';
import '../services/product_service.dart';

class ProductProvider extends ChangeNotifier {
  final ProductService _productService;
  ProductProvider({ProductService? productService})
      : _productService = productService ?? ProductService();

  List<ProductModel> _products = [];
  List<ProductModel> _featuredProducts = [];
  bool _isLoading = false;
  bool _isLoadingMore = false;
  bool _isFeaturedLoading = false;
  String? _error;
  int _currentPage = 0;
  bool _hasMore = true;
  String? _activeCategoryId;
  String _activeSearchQuery = '';
  bool _semanticMode = false;
  String _activeSort = 'name_asc';

  List<ProductModel> get products => _products;
  List<ProductModel> get featuredProducts => _featuredProducts;
  bool get isLoading => _isLoading;
  bool get isLoadingMore => _isLoadingMore;
  bool get isFeaturedLoading => _isFeaturedLoading;
  bool get hasMore => _hasMore;
  String? get error => _error;
  String get activeSearchQuery => _activeSearchQuery;
  bool get semanticMode => _semanticMode;
  String get activeSort => _activeSort;

  Future<void> fetchProducts({String sort = 'name_asc'}) async {
    _isLoading = true; _error = null; _activeCategoryId = null; _activeSearchQuery = '';
    _activeSort = sort;
    _currentPage = 0; _hasMore = true; _semanticMode = false; notifyListeners();
    try {
      final page = await _productService.getProducts(page: 0, sort: sort);
      _products = page.products; _currentPage = page.page; _hasMore = page.hasMore;
    } catch (e) { _error = e.toString(); }
    _isLoading = false; notifyListeners();
  }

  Future<void> fetchProductsByCategory(String categoryId, {String sort = 'name_asc'}) async {
    _isLoading = true; _error = null; _activeCategoryId = categoryId; _activeSearchQuery = '';
    _activeSort = sort;
    _currentPage = 0; _hasMore = true; _semanticMode = false; notifyListeners();
    try {
      final page = await _productService.getProductsByCategory(categoryId, page: 0, sort: sort);
      _products = page.products; _currentPage = page.page; _hasMore = page.hasMore;
    } catch (e) { _error = e.toString(); }
    _isLoading = false; notifyListeners();
  }

  /// Uses the backend's paginated keyword-search endpoint.
  Future<void> searchProducts(String query) async {
    final normalized = query.trim();
    if (normalized.isEmpty) {
      if (_activeCategoryId != null) {
        await fetchProductsByCategory(_activeCategoryId!, sort: _activeSort);
      } else {
        await fetchProducts(sort: _activeSort);
      }
      return;
    }
    _isLoading = true; _error = null; _activeSearchQuery = normalized;
    _semanticMode = false; _currentPage = 0; _hasMore = true; notifyListeners();
    try {
      final page = await _productService.searchProducts(normalized, page: 0);
      _products = page.products; _currentPage = page.page; _hasMore = page.hasMore;
    } catch (e) { _error = e.toString(); }
    _isLoading = false; notifyListeners();
  }

  /// Uses the backend's local embedding + pgvector semantic-search endpoint.
  /// No external AI API is required.
  Future<void> semanticSearchProducts(String query) async {
    final normalized = query.trim();
    if (normalized.isEmpty) {
      await fetchProducts(sort: _activeSort);
      return;
    }
    _isLoading = true; _error = null; _activeSearchQuery = normalized;
    _activeCategoryId = null; _semanticMode = true; _currentPage = 0; _hasMore = false; notifyListeners();
    try {
      _products = await _productService.semanticSearchProducts(normalized);
    } catch (e) { _error = e.toString(); _products = []; }
    _isLoading = false; notifyListeners();
  }

  Future<void> fetchMoreProducts() async {
    if (_isLoadingMore || !_hasMore || _isLoading) return;
    _isLoadingMore = true; notifyListeners();
    try {
      if (_semanticMode) {
        _isLoadingMore = false;
        return;
      }
      final nextPage = _currentPage + 1;
      final ProductPage page;
      if (_activeSearchQuery.isNotEmpty) {
        page = await _productService.searchProducts(_activeSearchQuery, page: nextPage);
      } else if (_activeCategoryId != null) {
        page = await _productService.getProductsByCategory(_activeCategoryId!, page: nextPage, sort: _activeSort);
      } else {
        page = await _productService.getProducts(page: nextPage, sort: _activeSort);
      }
      _products = [..._products, ...page.products];
      _currentPage = page.page; _hasMore = page.hasMore;
    } catch (e) { _error = e.toString(); }
    _isLoadingMore = false; notifyListeners();
  }

  Future<void> fetchFeaturedProducts() async {
    _isFeaturedLoading = true; notifyListeners();
    try { _featuredProducts = await _productService.getFeaturedProducts(); _error = null; }
    catch (e) { _error = e.toString(); }
    _isFeaturedLoading = false; notifyListeners();
  }

  Future<ProductModel?> fetchProductById(String id) async {
    try { return await _productService.getProductById(id); }
    catch (e) { _error = e.toString(); notifyListeners(); return null; }
  }
  Future<List<ProductModel>> fetchProductsByIds(List<String> ids) => _productService.getProductsByIds(ids);
  Future<ProductModel?> fetchProductByBarcode(String barcode) => _productService.getProductByBarcode(barcode);
}