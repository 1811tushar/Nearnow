import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:speech_to_text/speech_to_text.dart';
import '../../../l10n/app_localizations.dart';
import '../providers/product_provider.dart';
import '../models/product_model.dart';
import '../widgets/product_card.dart';
import '../../../core/widgets/empty_state_widget.dart';
import '../../../core/widgets/shimmer_loading_widget.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_radius.dart';
import '../../../core/constants/app_spacing.dart';
import '../../wishlist/providers/wishlist_provider.dart';
import '../../auth/providers/auth_provider.dart';
import '../../category/providers/category_provider.dart';

class ProductListPage extends StatefulWidget {
  final String? categoryId;
  final String? initialSearchQuery;
  final bool semanticSearch;
  const ProductListPage({super.key, this.categoryId, this.initialSearchQuery, this.semanticSearch = false});

  @override
  State<ProductListPage> createState() => _ProductListPageState();
}

// Sort option keys match exactly what ProductController.buildSort()
// understands on the backend — this list is the single source of
// truth for both the label shown in the sheet and the value sent
// over the wire, so the two can never drift out of sync.
const List<Map<String, String>> _sortOptions = [
  {'key': 'name_asc', 'label': 'Name (A-Z)'},
  {'key': 'price_asc', 'label': 'Price: Low to High'},
  {'key': 'price_desc', 'label': 'Price: High to Low'},
  {'key': 'rating_desc', 'label': 'Rating: High to Low'},
];

class _ProductListPageState extends State<ProductListPage> {
  String _searchQuery = '';
  late final TextEditingController _searchController;
  late final ScrollController _scrollController;
  RangeValues _priceRange = const RangeValues(0, 10000);

  // Filter-sheet selections that live for the whole page (not just
  // inside the sheet) — these are what actually get sent to the
  // backend when "Apply Filters" is tapped.
  String? _selectedCategoryId;
  String _selectedSort = 'name_asc';

  final SpeechToText _speech = SpeechToText();
  bool _speechAvailable = false;
  bool _isListening = false;
  Timer? _searchDebounce;

  @override
  void initState() {
    super.initState();
    _searchQuery = widget.initialSearchQuery ?? '';
    _searchController = TextEditingController(text: _searchQuery);
    _selectedCategoryId = widget.categoryId;
    _scrollController = ScrollController()..addListener(_onScroll);
    _initSpeech();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      final provider = context.read<ProductProvider>();
      if (_searchQuery.trim().isNotEmpty) {
        if (widget.semanticSearch) {
          provider.semanticSearchProducts(_searchQuery);
        } else {
          provider.searchProducts(_searchQuery);
        }
      } else if (widget.categoryId != null) {
        provider.fetchProductsByCategory(widget.categoryId!, sort: _selectedSort);
      } else {
        provider.fetchProducts(sort: _selectedSort);
      }

      final uid = context.read<AuthProvider>().user?.uid;
      if (uid != null) {
        context.read<WishlistProvider>().fetchWishlist(uid);
      }

      // Category chips need the top-level category list — reuse it if
      // Home already loaded it, otherwise fetch it once here.
      final categoryProvider = context.read<CategoryProvider>();
      if (categoryProvider.categories.isEmpty) {
        categoryProvider.fetchTopLevelCategories();
      }
    });
  }

  void _onScroll() {
    // Load the next page a little before the person actually hits the
    // bottom, so more items are already in place by the time they get
    // there — avoids a visible pause mid-scroll.
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 400) {
      context.read<ProductProvider>().fetchMoreProducts();
    }
  }

  Future<void> _initSpeech() async {
    _speechAvailable = await _speech.initialize(
      onStatus: (status) {
        // The plugin reports "done"/"notListening" when it stops on its
        // own (e.g. after a pause in speech) — reflect that in the UI.
        if (status == 'done' || status == 'notListening') {
          if (mounted) setState(() => _isListening = false);
        }
      },
      onError: (_) {
        if (mounted) setState(() => _isListening = false);
      },
    );
    if (mounted) setState(() {});
  }

  Future<void> _toggleListening() async {
    if (_isListening) {
      await _speech.stop();
      setState(() => _isListening = false);
      return;
    }

    if (!_speechAvailable) {
      final l10n = AppLocalizations.of(context)!;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.speechNotAvailable),
        ),
      );
      return;
    }

    setState(() => _isListening = true);
    await _speech.listen(
      onResult: (result) {
        setState(() {
          _searchController.text = result.recognizedWords;
          _searchController.selection = TextSelection.collapsed(
            offset: _searchController.text.length,
          );
          _searchQuery = result.recognizedWords;
        });
      },
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    _searchDebounce?.cancel();
    _scrollController.dispose();
    _speech.stop();
    super.dispose();
  }

  // Price stays a local/client-side filter (same as before) — it narrows
  // whatever page is currently loaded. Category and sort, by contrast,
  // now go all the way to the backend via _applyServerFilters below,
  // since those change WHICH products and in WHAT order, not just which
  // of the already-loaded ones to hide.
  List<ProductModel> _applyPriceFilter(List<ProductModel> products) {
    return products.where((product) {
      final price = product.effectivePrice;
      return price >= _priceRange.start && price <= _priceRange.end;
    }).toList();
  }

  void _applyServerFilters() {
    final provider = context.read<ProductProvider>();
    if (_selectedCategoryId != null) {
      provider.fetchProductsByCategory(_selectedCategoryId!, sort: _selectedSort);
    } else {
      provider.fetchProducts(sort: _selectedSort);
    }
  }

  void _openFilterSheet() {
    final categories = context.read<CategoryProvider>().categories;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppRadius.sheet),
        ),
      ),
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setSheetState) {
            final l10n = AppLocalizations.of(context)!;
            return Padding(
              padding: EdgeInsets.only(
                left: AppSpacing.lg,
                right: AppSpacing.lg,
                top: AppSpacing.lg,
                bottom: MediaQuery.of(context).viewInsets.bottom + AppSpacing.lg,
              ),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(l10n.filters, style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: AppSpacing.lg),

                    // ---- Category chips ----
                    Text('Category',
                        style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                            )),
                    const SizedBox(height: AppSpacing.sm),
                   
                                       Wrap(
                      spacing: AppSpacing.sm,
                      runSpacing: AppSpacing.sm,
                      children: [
                        ChoiceChip(
                          label: const Text('All'),
                          selected: _selectedCategoryId == null,
                          selectedColor: AppColors.primary,
                          checkmarkColor: AppColors.secondary,
                          backgroundColor: AppColors.secondary,
                          side: BorderSide(color: Colors.grey.shade300),
                          labelStyle: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: _selectedCategoryId == null
                                ? AppColors.secondary
                                : Colors.black87,
                          ),
                          onSelected: (_) {
                            setSheetState(() => _selectedCategoryId = null);
                          },
                        ),
                        for (final category in categories)
                          ChoiceChip(
                            label: Text(category.name),
                            selected: _selectedCategoryId == category.id,
                            selectedColor: AppColors.primary,
                            checkmarkColor: AppColors.secondary,
                            backgroundColor: AppColors.secondary,
                            side: BorderSide(color: Colors.grey.shade300),
                            labelStyle: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: _selectedCategoryId == category.id
                                  ? AppColors.secondary
                                  : Colors.black87,
                            ),
                            onSelected: (_) {
                              setSheetState(() => _selectedCategoryId = category.id);
                            },
                          ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.lg),

                    // ---- Sort options ----
                    

                    // ---- Sort options ----
                    Text('Sort By',
                        style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                            )),
                    const SizedBox(height: AppSpacing.sm),
                                        Wrap(
                      spacing: AppSpacing.sm,
                      runSpacing: AppSpacing.sm,
                      children: [
                        for (final option in _sortOptions)
                          ChoiceChip(
                            label: Text(option['label']!),
                            selected: _selectedSort == option['key'],
                            selectedColor: AppColors.primary,
                            checkmarkColor: AppColors.secondary,
                            backgroundColor: AppColors.secondary,
                            side: BorderSide(color: Colors.grey.shade300),
                            labelStyle: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: _selectedSort == option['key']
                                  ? AppColors.secondary
                                  : Colors.black87,
                            ),
                            onSelected: (_) {
                              setSheetState(() => _selectedSort = option['key']!);
                            },
                          ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.lg),

                    // ---- Price range (existing behavior, unchanged) ----

                    // ---- Price range (existing behavior, unchanged) ----
                    Text(l10n.priceRange,
                        style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                            )),
                    RangeSlider(
                      values: _priceRange,
                      min: 0,
                      max: 10000,
                      divisions: 20,
                      labels: RangeLabels(
                        "₹${_priceRange.start.round()}",
                        "₹${_priceRange.end.round()}",
                      ),
                      onChanged: (values) {
                        setSheetState(() => _priceRange = values);
                      },
                    ),
                    const SizedBox(height: AppSpacing.md),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: () {
                          setState(() {});
                          Navigator.pop(context);
                          _applyServerFilters();
                        },
                        child: Text(l10n.applyFilters),
                      ),
                    ),
                    TextButton(
                      onPressed: () {
                        setSheetState(() {
                          _selectedCategoryId = null;
                          _selectedSort = 'name_asc';
                          _priceRange = const RangeValues(0, 10000);
                        });
                      },
                      child: Text(l10n.clearAllFilters),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final productProvider = context.watch<ProductProvider>();
    final filteredProducts = _applyPriceFilter(productProvider.products);
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.categoryId != null ? l10n.products : l10n.allProducts),
        actions: [
          IconButton(
            icon: const Icon(Icons.filter_list),
            onPressed: _openFilterSheet,
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(AppSpacing.md),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: l10n.searchProductsHint,
                prefixIcon: const Icon(Icons.search, color: AppColors.grey),
                suffixIcon: IconButton(
                  icon: Icon(
                    _isListening ? Icons.mic : Icons.mic_none,
                    color: _isListening ? AppColors.primary : AppColors.grey,
                  ),
                  onPressed: _toggleListening,
                ),
                isDense: true,
              ),
              onChanged: (value) {
                setState(() => _searchQuery = value);
                _searchDebounce?.cancel();
                _searchDebounce = Timer(const Duration(milliseconds: 350), () {
                  if (!mounted) return;
                  context.read<ProductProvider>().searchProducts(value);
                });
              },
            ),
          ),
          Expanded(
            child: productProvider.isLoading
                ? const ShimmerProductGrid()
                : productProvider.error != null
                    ? EmptyStateWidget(
                        icon: Icons.error_outline,
                        title: l10n.somethingWentWrong,
                        subtitle: productProvider.error,
                      )
                    : filteredProducts.isEmpty
                        ? EmptyStateWidget(
                            icon: Icons.search_off,
                            title: l10n.noProductsFound,
                            subtitle: l10n.tryAdjustingSearch,
                          )
                        : RefreshIndicator(
                            onRefresh: () => _selectedCategoryId != null
                                ? productProvider.fetchProductsByCategory(
                                    _selectedCategoryId!, sort: _selectedSort)
                                : productProvider.fetchProducts(sort: _selectedSort),
                            child: GridView.builder(
                            controller: _scrollController,
                            padding: const EdgeInsets.all(AppSpacing.md),
                            gridDelegate:
                                const SliverGridDelegateWithFixedCrossAxisCount(
                              crossAxisCount: 2,
                              mainAxisSpacing: AppSpacing.md,
                              crossAxisSpacing: AppSpacing.md,
                              childAspectRatio: 0.58,
                            ),
                            itemCount: filteredProducts.length +
                                (productProvider.isLoadingMore ? 2 : 0),
                            itemBuilder: (context, index) {
                              if (index >= filteredProducts.length) {
                                return const Center(
                                  child: Padding(
                                    padding: EdgeInsets.all(AppSpacing.md),
                                    child: CircularProgressIndicator(),
                                  ),
                                );
                              }
                              return ProductCard(product: filteredProducts[index]);
                            },
                            ),
                          ),
          ),
        ],
      ),
    );
  }
}