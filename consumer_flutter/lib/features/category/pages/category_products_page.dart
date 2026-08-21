import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/category_model.dart';
import '../providers/category_provider.dart';
import '../../products/providers/product_provider.dart';
import '../../products/widgets/product_card.dart';
import '../../../core/widgets/empty_state_widget.dart';
import '../../../core/widgets/shimmer_loading_widget.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../l10n/app_localizations.dart';

/// Blinkit-style two-pane category browser: a scrollable icon rail of every
/// top-level category on the left, and a live product grid for whichever
/// category is currently selected on the right. Tapping a rail item swaps
/// the grid in place — no navigation, matching how Blinkit's own category
/// screen behaves.
class CategoryProductsPage extends StatefulWidget {
  final CategoryModel category;

  const CategoryProductsPage({
    super.key,
    required this.category,
  });

  @override
  State<CategoryProductsPage> createState() => _CategoryProductsPageState();
}

class _CategoryProductsPageState extends State<CategoryProductsPage> {
  late CategoryModel _selectedCategory;

  @override
  void initState() {
    super.initState();
    _selectedCategory = widget.category;

    WidgetsBinding.instance.addPostFrameCallback((_) {
      final categoryProvider = context.read<CategoryProvider>();
      // Make sure the rail has data even on a deep link / cold start,
      // where Home may not have populated it yet.
      if (categoryProvider.categories.isEmpty) {
        categoryProvider.fetchTopLevelCategories();
      }
      context
          .read<ProductProvider>()
          .fetchProductsByCategory(_selectedCategory.id);
    });
  }

  void _selectCategory(CategoryModel category) {
    if (category.id == _selectedCategory.id) return;
    setState(() => _selectedCategory = category);
    context.read<ProductProvider>().fetchProductsByCategory(category.id);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final categoryProvider = context.watch<CategoryProvider>();
    final productProvider = context.watch<ProductProvider>();
    final rail = categoryProvider.categories.isNotEmpty
        ? categoryProvider.categories
        : [_selectedCategory];

    return Scaffold(
      appBar: AppBar(title: Text(_selectedCategory.name)),
      body: Row(
        children: [
          // Left icon rail
          Container(
            width: 88,
            color: AppColors.background,
            child: ListView.builder(
              itemCount: rail.length,
              itemBuilder: (context, index) {
                final category = rail[index];
                final isSelected = category.id == _selectedCategory.id;

                return InkWell(
                  onTap: () => _selectCategory(category),
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                        vertical: AppSpacing.md, horizontal: AppSpacing.xs),
                    decoration: BoxDecoration(
                      color: isSelected
                          ? AppColors.secondary
                          : Colors.transparent,
                      border: Border(
                        left: BorderSide(
                          color: isSelected
                              ? AppColors.primary
                              : Colors.transparent,
                          width: 3,
                        ),
                      ),
                    ),
                    child: Column(
                      children: [
                        CircleAvatar(
                          radius: 24,
                          backgroundColor: AppColors.secondary,
                          backgroundImage: category.imageUrl.isNotEmpty
                              ? NetworkImage(category.imageUrl)
                              : null,
                          child: category.imageUrl.isEmpty
                              ? const Icon(Icons.category,
                                  color: AppColors.grey)
                              : null,
                        ),
                        const SizedBox(height: AppSpacing.xs),
                        Text(
                          category.name,
                          textAlign: TextAlign.center,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: isSelected
                                ? FontWeight.bold
                                : FontWeight.normal,
                            color: isSelected
                                ? AppColors.primary
                                : AppColors.grey,
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),

          // Right product grid
          Expanded(
            child: productProvider.isLoading
                ? const ShimmerProductGrid()
                : productProvider.error != null
                    ? EmptyStateWidget(
                        icon: Icons.error_outline,
                        title: l10n.somethingWentWrong,
                        subtitle: productProvider.error,
                      )
                    : productProvider.products.isEmpty
                        ? EmptyStateWidget(
                            icon: Icons.inventory_2_outlined,
                            title: l10n.noProductsHereYet,
                            subtitle: l10n.checkBackSoon,
                          )
                        : GridView.builder(
                            padding: const EdgeInsets.all(AppSpacing.md),
                            gridDelegate:
                                const SliverGridDelegateWithFixedCrossAxisCount(
                              crossAxisCount: 2,
                              mainAxisSpacing: AppSpacing.md,
                              crossAxisSpacing: AppSpacing.md,
                              childAspectRatio: 0.58,
                            ),
                            itemCount: productProvider.products.length,
                            itemBuilder: (context, index) {
                              return ProductCard(
                                  product: productProvider.products[index]);
                            },
                          ),
          ),
        ],
      ),
    );
  }
}