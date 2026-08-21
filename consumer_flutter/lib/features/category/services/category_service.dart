import '../../../core/network/api_client.dart';
import '../models/category_model.dart';

class CategoryService {
  final ApiClient _client;

  CategoryService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<List<CategoryModel>> fetchTopLevelCategories() async {
    final data = await _client.get('/categories/top-level');
    return (data as List)
        .map((j) => CategoryModel.fromApi(j as Map<String, dynamic>))
        .toList();
  }

  Future<List<CategoryModel>> fetchSubCategories(String parentId) async {
    final data = await _client.get('/categories/$parentId/subcategories');
    return (data as List)
        .map((j) => CategoryModel.fromApi(j as Map<String, dynamic>))
        .toList();
  }
}