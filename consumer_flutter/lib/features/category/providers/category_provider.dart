
import 'package:flutter/material.dart';
import '../models/category_model.dart';
import '../services/category_service.dart';

class CategoryProvider extends ChangeNotifier {
  final CategoryService _categoryService;

  CategoryProvider({CategoryService? categoryService})
      : _categoryService = categoryService ?? CategoryService();

  List<CategoryModel> _categories = [];
  List<CategoryModel> _subCategories = [];
  bool _isLoading = false;
  String? _error;

  List<CategoryModel> get categories => _categories;
  List<CategoryModel> get subCategories => _subCategories;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> fetchTopLevelCategories() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _categories = await _categoryService.fetchTopLevelCategories();
    } catch (e) {
      _error = e.toString();
    }

    _isLoading = false;
    notifyListeners();
  }

  // NOTE: fetchSubCategories/subCategories/clearSubCategories below have no
  // UI caller yet. This isn't accidental dead code left over from a
  // refactor — the `categories` collection's schema (parentCategoryId +
  // sortOrder, which already has a composite index deployed) was clearly
  // built to support a nested category browsing UI. Wiring that UI up
  // (a subcategory grid, breadcrumbs, back-stack handling) is a new
  // navigation flow that deserves its own dedicated pass with real user
  // testing, not a bolt-on at the tail end of an already-large fix pass —
  // so this is a deliberate deferral, not an oversight.
  Future<void> fetchSubCategories(String parentId) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _subCategories = await _categoryService.fetchSubCategories(parentId);
    } catch (e) {
      _error = e.toString();
    }

    _isLoading = false;
    notifyListeners();
  }

  void clearSubCategories() {
    _subCategories = [];
    notifyListeners();
  }
}