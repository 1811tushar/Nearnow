import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../models/product_model.dart';

class ProductPage {
  final List<ProductModel> products;
  final int page;
  final bool hasMore;

  ProductPage({required this.products, required this.page, required this.hasMore});
}

class ProductService {
  final ApiClient _client;

  ProductService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  static const int defaultPageSize = 20;

  ProductPage _parsePage(Map<String, dynamic> data) {
    final content = data['content'] as List;
    return ProductPage(
      products: content
          .map((j) => ProductModel.fromApi(j as Map<String, dynamic>))
          .toList(),
      page: data['page'] as int,
      hasMore: data['hasMore'] as bool,
    );
  }

  Future<ProductPage> getProducts({
    int page = 0,
    int size = defaultPageSize,
    String sort = 'name_asc',
  }) async {
    final data = await _client.get('/products?page=$page&size=$size&sort=$sort');
    return _parsePage(data as Map<String, dynamic>);
  }

  Future<ProductPage> getProductsByCategory(
    String categoryId, {
    int page = 0,
    int size = defaultPageSize,
    String sort = 'name_asc',
  }) async {
    final data = await _client
        .get('/products/category/$categoryId?page=$page&size=$size&sort=$sort');
    return _parsePage(data as Map<String, dynamic>);
  }

  Future<ProductPage> searchProducts(
    String query, {
    int page = 0,
    int size = defaultPageSize,
  }) async {
    final data = await _client
        .get('/products/search?q=${Uri.encodeQueryComponent(query)}&page=$page&size=$size');
    return _parsePage(data as Map<String, dynamic>);
  }

  /// Natural-language search backed by the server's local embedding model + pgvector.
  /// This path is intentionally non-paginated: the backend returns a ranked top-N set.
  Future<List<ProductModel>> semanticSearchProducts(
    String query, {
    int limit = 20,
  }) async {
    final data = await _client.get(
      '/products/semantic-search?q=${Uri.encodeQueryComponent(query)}&limit=$limit',
    );
    return (data as List)
        .map((j) => ProductModel.fromApi(j as Map<String, dynamic>))
        .toList();
  }

  Future<List<ProductModel>> getProductsByIds(List<String> ids) async {
    if (ids.isEmpty) return [];
    final data = await _client.get('/products/batch?ids=${ids.join(',')}');
    return (data as List)
        .map((j) => ProductModel.fromApi(j as Map<String, dynamic>))
        .toList();
  }

  Future<ProductModel?> getProductById(String id) async {
    try {
      final data = await _client.get('/products/$id');
      return ProductModel.fromApi(data as Map<String, dynamic>);
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }

  Future<List<ProductModel>> getFeaturedProducts() async {
    final data = await _client.get('/products/featured');
    return (data as List)
        .map((j) => ProductModel.fromApi(j as Map<String, dynamic>))
        .toList();
  }

  Future<ProductModel?> getProductByBarcode(String barcode) async {
    try {
      final data = await _client.get('/products/barcode/$barcode');
      return ProductModel.fromApi(data as Map<String, dynamic>);
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }
}