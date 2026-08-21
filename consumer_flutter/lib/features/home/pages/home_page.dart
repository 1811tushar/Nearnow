import 'dart:async';

import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:provider/provider.dart';
import '../../auth/providers/auth_provider.dart';
import '../../address/providers/address_provider.dart';
import '../../address/pages/address_list_page.dart';
import '../../category/providers/category_provider.dart';
import '../../category/pages/category_products_page.dart';
import '../../products/providers/product_provider.dart';
import '../../products/models/product_model.dart';
import '../../products/pages/product_list_page.dart';
import '../../products/pages/barcode_scanner_page.dart';
import '../../products/pages/image_search_page.dart';
import '../../products/widgets/product_card.dart';
import '../../../core/widgets/section_header.dart';
import '../../../core/widgets/shimmer_loading_widget.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_radius.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../l10n/app_localizations.dart';
import '../../../core/widgets/language_picker_sheet.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final PageController _promoController = PageController();
  Timer? _searchHintTimer;
  int _searchHintIndex = 0;
  int _promoIndex = 0;

  List<(String, String, List<Color>)> _promos(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return [
      (l10n.promoNewArrivalsTitle, l10n.promoNewArrivalsSubtitle,
          [AppColors.primary, const Color(0xFF333333)]),
      (l10n.promoFreshPicksTitle, l10n.promoFreshPicksSubtitle,
          [const Color(0xFF2E7D32), const Color(0xFF66BB6A)]),
      (l10n.promoQuickEssentialsTitle, l10n.promoQuickEssentialsSubtitle,
          [const Color(0xFF1565C0), const Color(0xFF42A5F5)]),
    ];
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final uid = context.read<AuthProvider>().user?.uid;
      if (uid != null) {
        context.read<AddressProvider>().fetchAddresses(uid);
      }
      context.read<CategoryProvider>().fetchTopLevelCategories();
      context.read<ProductProvider>().fetchFeaturedProducts();
      // Also fetch the full catalog so the Bestsellers preview tiles
      // below have real per-category product thumbnails to show.
      context.read<ProductProvider>().fetchProducts();
    });
    _searchHintTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      if (mounted) setState(() => _searchHintIndex++);
    });
  }

  @override
  void dispose() {
    _searchHintTimer?.cancel();
    _promoController.dispose();
    super.dispose();
  }

  Map<String, List<ProductModel>> _groupByCategory(
      List<ProductModel> products) {
    final Map<String, List<ProductModel>> grouped = {};
    for (final product in products) {
      grouped.putIfAbsent(product.categoryId, () => []).add(product);
    }
    return grouped;
  }

  Widget _buildCategoryStrip(BuildContext context,
      CategoryProvider categoryProvider) {
    if (categoryProvider.categories.isEmpty) return const SizedBox.shrink();

    return Column(
      children: [
        SectionHeader(title: AppLocalizations.of(context)!.categories),
        SizedBox(
          height: 90,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
            itemCount: categoryProvider.categories.length,
            itemBuilder: (context, index) {
              final category = categoryProvider.categories[index];
              return Padding(
                padding: const EdgeInsets.only(right: AppSpacing.md),
                child: GestureDetector(
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => CategoryProductsPage(category: category),
                    ),
                  ),
                  child: Column(
                    children: [
                      CircleAvatar(
                        radius: 28,
                        backgroundColor: AppColors.background,
                        backgroundImage: category.imageUrl.isNotEmpty
                            ? NetworkImage(category.imageUrl)
                            : null,
                        child: category.imageUrl.isEmpty
                            ? const Icon(Icons.category, color: AppColors.grey)
                            : null,
                      ),
                      const SizedBox(height: AppSpacing.xs),
                      SizedBox(
                        width: 64,
                        child: Text(
                          category.name,
                          textAlign: TextAlign.center,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.labelSmall,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
        const SizedBox(height: AppSpacing.lg),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final addressProvider = context.watch<AddressProvider>();
    final categoryProvider = context.watch<CategoryProvider>();
    final productProvider = context.watch<ProductProvider>();
    final selectedAddress = addressProvider.selectedAddress;
    final productsByCategory = _groupByCategory(productProvider.products);

    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(
        title: InkWell(
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const AddressListPage()),
            );
          },
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.location_on, size: 20),
              const SizedBox(width: 4),
              Expanded(
                child: Text(
                  selectedAddress != null
                      ? l10n.deliverTo(selectedAddress.label)
                      : l10n.selectDeliveryAddress,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 16),
                ),
              ),
              const Icon(Icons.arrow_drop_down),
            ],
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.language),
            tooltip: l10n.language,
            onPressed: () => showLanguagePicker(context),
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.only(bottom: AppSpacing.xxl),
          children: [
            // Search bar
            Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: Row(
                children: [
                  Expanded(
                    child: GestureDetector(
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                              builder: (_) => const ProductListPage()),
                        );
                      },
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.lg, vertical: AppSpacing.md),
                        decoration: BoxDecoration(
                          color: AppColors.background,
                          borderRadius: BorderRadius.circular(AppRadius.button),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.search, color: AppColors.grey),
                            const SizedBox(width: AppSpacing.sm),
                            Expanded(
                              child: AnimatedSwitcher(
                                duration: const Duration(milliseconds: 250),
                                child: Text(
                                  categoryProvider.categories.isEmpty
                                      ? l10n.searchProductsHint
                                      : l10n.searchCategoryHint(
                                          categoryProvider.categories[
                                                  _searchHintIndex %
                                                      categoryProvider
                                                          .categories.length]
                                              .name),
                                  key: ValueKey(_searchHintIndex),
                                  style: Theme.of(context)
                                      .textTheme
                                      .bodyMedium
                                      ?.copyWith(color: AppColors.grey),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  GestureDetector(
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => const BarcodeScannerPage()),
                      );
                    },
                    child: Container(
                      padding: const EdgeInsets.all(AppSpacing.md),
                      decoration: BoxDecoration(
                        color: AppColors.background,
                        borderRadius: BorderRadius.circular(AppRadius.button),
                      ),
                      child: const Icon(
                        Icons.qr_code_scanner,
                        color: AppColors.grey,
                      ),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  GestureDetector(
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => const ImageSearchPage()),
                      );
                    },
                    child: Container(
                      padding: const EdgeInsets.all(AppSpacing.md),
                      decoration: BoxDecoration(
                        color: AppColors.background,
                        borderRadius: BorderRadius.circular(AppRadius.button),
                      ),
                      child: const Icon(
                        Icons.camera_alt_outlined,
                        color: AppColors.grey,
                      ),
                    ),
                  ),
                ],
              ),
            ),

            _buildCategoryStrip(context, categoryProvider),

            // Swipeable promotional banners.
            SizedBox(
              height: 160,
              child: PageView.builder(
                controller: _promoController,
                itemCount: _promos(context).length,
                onPageChanged: (index) => setState(() => _promoIndex = index),
                itemBuilder: (context, index) {
                  final promo = _promos(context)[index];
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
                    child: Container(
                      padding: const EdgeInsets.all(AppSpacing.lg),
                      alignment: Alignment.centerLeft,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(AppRadius.card),
                        gradient: LinearGradient(
                          colors: promo.$3,
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                      ),
                      child: Text(
                        '${promo.$1}\n${promo.$2}',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          height: 1.3,
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(
                _promos(context).length,
                (index) => AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  width: _promoIndex == index ? 18 : 6,
                  height: 6,
                  margin: const EdgeInsets.all(3),
                  decoration: BoxDecoration(
                    color: _promoIndex == index
                        ? AppColors.primary
                        : AppColors.grey.withValues(alpha: 0.35),
                    borderRadius: BorderRadius.circular(99),
                  ),
                ),
              ),
            ),

            const SizedBox(height: AppSpacing.md),

            // Offers strip — currently decorative (no real coupon logic
            // wired in yet; that's the Coupons feature, still pending).
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              child: Row(
                children: [
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(AppSpacing.sm),
                      decoration: BoxDecoration(
                        color: AppColors.background,
                        borderRadius: BorderRadius.circular(AppRadius.button),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.percent,
                              size: 18, color: AppColors.success),
                          const SizedBox(width: AppSpacing.xs),
                          Expanded(
                            child: Text(
                              "FLAT ₹50 OFF\non orders above ₹249",
                              style: Theme.of(context).textTheme.labelSmall,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(AppSpacing.sm),
                      decoration: BoxDecoration(
                        color: AppColors.background,
                        borderRadius: BorderRadius.circular(AppRadius.button),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.local_shipping_outlined,
                              size: 18, color: AppColors.success),
                          const SizedBox(width: AppSpacing.xs),
                          Expanded(
                            child: Text(
                              "FREE delivery\non all orders",
                              style: Theme.of(context).textTheme.labelSmall,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: AppSpacing.lg),

            // Bestsellers — compact per-category preview tiles
            if (productsByCategory.isNotEmpty &&
                categoryProvider.categories.isNotEmpty) ...[
              SectionHeader(title: l10n.bestsellers),
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  mainAxisSpacing: AppSpacing.md,
                  crossAxisSpacing: AppSpacing.md,
                  childAspectRatio: 1.1,
                ),
                itemCount: categoryProvider.categories.length > 6
                    ? 6
                    : categoryProvider.categories.length,
                itemBuilder: (context, index) {
                  final category = categoryProvider.categories[index];
                  final categoryProducts =
                      productsByCategory[category.id] ?? [];

                  if (categoryProducts.isEmpty) return const SizedBox();

                  final previewItems = categoryProducts.take(4).toList();
                  final remaining = categoryProducts.length - 4;

                  return GestureDetector(
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) =>
                              CategoryProductsPage(category: category),
                        ),
                      );
                    },
                    child: Container(
                      padding: const EdgeInsets.all(AppSpacing.sm),
                      decoration: BoxDecoration(
                        color: AppColors.secondary,
                        border: Border.all(color: AppColors.background),
                        borderRadius: BorderRadius.circular(AppRadius.card),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: GridView.builder(
                              physics: const NeverScrollableScrollPhysics(),
                              gridDelegate:
                                  const SliverGridDelegateWithFixedCrossAxisCount(
                                crossAxisCount: 2,
                                mainAxisSpacing: 4,
                                crossAxisSpacing: 4,
                              ),
                              itemCount: previewItems.length,
                              itemBuilder: (context, i) {
                                final p = previewItems[i];
                                return ClipRRect(
                                  borderRadius: BorderRadius.circular(4),
                                  child: p.images.isNotEmpty
                                      ? CachedNetworkImage(
                                          imageUrl: p.images.first,
                                          fit: BoxFit.cover,
                                          errorWidget: (_, __, ___) =>
                                              Container(
                                                  color:
                                                      AppColors.background),
                                        )
                                      : Container(
                                          color: AppColors.background),
                                );
                              },
                            ),
                          ),
                          const SizedBox(height: AppSpacing.xs),
                          if (remaining > 0)
                            Text(
                              "+$remaining more",
                              style: const TextStyle(
                                  fontSize: 10, color: AppColors.grey),
                            ),
                          Text(
                            category.name,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: Theme.of(context)
                                .textTheme
                                .bodyMedium
                                ?.copyWith(fontWeight: FontWeight.w600),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
              const SizedBox(height: AppSpacing.lg),
            ],

            // Featured products carousel
            SectionHeader(
              title: l10n.featuredProducts,
              onViewAll: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const ProductListPage()),
                );
              },
            ),
            SizedBox(
              height: 280,
              child: productProvider.featuredProducts.isEmpty
                  ? ListView.builder(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.symmetric(
                          horizontal: AppSpacing.lg),
                      itemCount: 4,
                      itemBuilder: (context, index) => Padding(
                        padding: const EdgeInsets.only(right: AppSpacing.md),
                        child: SizedBox(
                          width: 150,
                          child: ShimmerBox(
                              width: 150,
                              height: 260,
                              borderRadius: AppRadius.card),
                        ),
                      ),
                    )
                  : ListView.builder(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.symmetric(
                          horizontal: AppSpacing.lg),
                      itemCount: productProvider.featuredProducts.length,
                      itemBuilder: (context, index) {
                        final product =
                            productProvider.featuredProducts[index];
                        return Padding(
                          padding:
                              const EdgeInsets.only(right: AppSpacing.md),
                          child: ProductCard(product: product, width: 150),
                        );
                      },
                    ),
            ),

            const SizedBox(height: AppSpacing.lg),

            // Full category grid
            SectionHeader(title: l10n.shopByCategory),
            categoryProvider.isLoading
                ? const Padding(
                    padding: EdgeInsets.all(AppSpacing.xxl),
                    child: Center(child: CircularProgressIndicator()),
                  )
                : GridView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(AppSpacing.lg),
                    gridDelegate:
                        const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 3,
                      mainAxisSpacing: AppSpacing.md,
                      crossAxisSpacing: AppSpacing.md,
                    ),
                    itemCount: categoryProvider.categories.length,
                    itemBuilder: (context, index) {
                      final category = categoryProvider.categories[index];
                      return InkWell(
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) =>
                                  CategoryProductsPage(category: category),
                            ),
                          );
                        },
                        child: Column(
                          children: [
                            Expanded(
                              child: ClipRRect(
                                borderRadius:
                                    BorderRadius.circular(AppRadius.card),
                                child: CachedNetworkImage(
                                  imageUrl: category.imageUrl,
                                  fit: BoxFit.cover,
                                  errorWidget: (_, __, ___) =>
                                      const Icon(Icons.category, size: 40),
                                ),
                              ),
                            ),
                            const SizedBox(height: AppSpacing.xs),
                            Text(
                              category.name,
                              textAlign: TextAlign.center,
                              style: Theme.of(context).textTheme.bodyMedium,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ),
                      );
                    },
                  ),
          ],
        ),
      ),
    );
  }
}
