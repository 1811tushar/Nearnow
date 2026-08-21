import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:provider/provider.dart';

import '../models/product_model.dart';
import '../providers/product_provider.dart';

import '../../../core/widgets/loading_widget.dart';
import '../../../core/widgets/primary_button.dart';
import '../../../core/widgets/quantity_stepper.dart';
import '../../../core/widgets/rating_stars.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_radius.dart';
import '../../../core/constants/app_spacing.dart';

import '../../auth/providers/auth_provider.dart';
import '../../cart/models/cart_item_model.dart';
import '../../cart/providers/cart_provider.dart';
import '../../wishlist/providers/wishlist_provider.dart';
import '../../reviews/models/review_model.dart';
import '../../reviews/providers/review_provider.dart';
import '../../../l10n/app_localizations.dart';

class ProductDetailPage extends StatefulWidget {
  final String productId;

  const ProductDetailPage({
    super.key,
    required this.productId,
  });

  @override
  State<ProductDetailPage> createState() => _ProductDetailPageState();
}

class _ProductDetailPageState extends State<ProductDetailPage> {
  ProductModel? _product;

  bool _isLoading = true;
  String? _error;

  int _quantity = 1;
  int _imagePage = 0;
  final PageController _imageController = PageController();

  @override
  void initState() {
    super.initState();
    _loadProduct();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<ReviewProvider>().fetchReviews(widget.productId);
    });
  }

  @override
  void dispose() {
    _imageController.dispose();
    super.dispose();
  }

  Future<void> _loadProduct() async {
    final provider = context.read<ProductProvider>();

    final product = await provider.fetchProductById(widget.productId);

    if (!mounted) return;

    final l10n = AppLocalizations.of(context)!;
    setState(() {
      _product = product;
      _isLoading = false;
      _error = product == null ? l10n.productNotFound : null;
    });
  }

  void _addToCart(ProductModel product) {
    final l10n = AppLocalizations.of(context)!;
    final uid = context.read<AuthProvider>().user?.uid;
    if (uid == null) return;

    final cartItem = CartItemModel(
      id: '',
      productId: product.id,
      name: product.name,
      image: product.images.isNotEmpty ? product.images.first : '',
      price: product.effectivePrice,
      unit: product.unit,
      quantity: _quantity,
    );

    context.read<CartProvider>().addToCart(uid, cartItem);

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(l10n.addedQuantityToCart(_quantity, product.name)),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.button),
        ),
      ),
    );
  }

  void _openWriteReviewSheet(ProductModel product) {
    final uid = context.read<AuthProvider>().user?.uid;
    if (uid == null) return;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => _WriteReviewSheet(
        productId: product.id,
        uid: uid,
      ),
    ).then((_) {
      // The sheet only pops itself on a *successful* submission (see
      // _WriteReviewSheet._submit), so by the time we get here the
      // product's `rating`/`reviewCount` may have just changed in
      // Firestore. Re-fetch so this page's own `_product` state — which
      // is separate from ReviewProvider's state — doesn't keep showing
      // the old star rating until the person leaves and reopens the page.
      if (mounted) _loadProduct();
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(_product?.name ?? l10n.productDetails),
        actions: [
          if (_product != null)
            Consumer<WishlistProvider>(
              builder: (context, wishlistProvider, _) {
                final isSaved = wishlistProvider.isInWishlist(_product!.id);

                return IconButton(
                  icon: Icon(
                    isSaved ? Icons.favorite : Icons.favorite_border,
                    color: isSaved ? AppColors.error : null,
                  ),
                  onPressed: () {
                    final uid = context.read<AuthProvider>().user?.uid;
                    if (uid == null) return;
                    wishlistProvider.toggleWishlist(uid, _product!.id);
                  },
                );
              },
            ),
        ],
      ),
      body: _isLoading
          ? const LoadingWidget()
          : _error != null
              ? Center(
                  child: Text(
                    _error!,
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                )
              : _buildContent(_product!),
      bottomNavigationBar: (_isLoading || _error != null)
          ? null
          : _buildStickyAddToCartBar(_product!),
    );
  }

  Widget _buildContent(ProductModel product) {
    final l10n = AppLocalizations.of(context)!;
    final reviewProvider = context.watch<ReviewProvider>();

    return SingleChildScrollView(
      padding: const EdgeInsets.only(bottom: AppSpacing.xxl),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildImageGallery(product),
          Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  product.name,
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  product.unit,
                  style: Theme.of(context)
                      .textTheme
                      .bodyMedium
                      ?.copyWith(color: AppColors.grey),
                ),
                const SizedBox(height: AppSpacing.xs),
                Row(
                  children: [
                    RatingStars(rating: product.rating, size: 16),
                    const SizedBox(width: AppSpacing.xs),
                    Text(
                      l10n.ratingReviewsCount(product.rating.toStringAsFixed(1), reviewProvider.reviews.length),
                      style: Theme.of(context)
                          .textTheme
                          .bodySmall
                          ?.copyWith(color: AppColors.grey),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.md),
                Row(
                  children: [
                    if (product.discountPercent > 0) ...[
                      Text(
                        "₹${product.salePrice}",
                        style: Theme.of(context)
                            .textTheme
                            .headlineSmall
                            ?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(width: AppSpacing.sm),
                      Text(
                        "₹${product.price}",
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                              decoration: TextDecoration.lineThrough,
                              color: AppColors.grey,
                            ),
                      ),
                      const SizedBox(width: AppSpacing.sm),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.sm, vertical: 2),
                        decoration: BoxDecoration(
                          color: AppColors.error,
                          borderRadius: BorderRadius.circular(AppRadius.chip),
                        ),
                        child: Text(
                          "${product.discountPercent}% OFF",
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ] else
                      Text(
                        "₹${product.price}",
                        style: Theme.of(context)
                            .textTheme
                            .headlineSmall
                            ?.copyWith(fontWeight: FontWeight.bold),
                      ),
                  ],
                ),
                const SizedBox(height: AppSpacing.md),
                Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: AppSpacing.md, vertical: AppSpacing.xs),
                  decoration: BoxDecoration(
                    color: (product.stock > 0
                            ? AppColors.success
                            : AppColors.error)
                        .withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(AppRadius.chip),
                  ),
                  child: Text(
                    product.stock > 0
                        ? l10n.inStockCount(product.stock)
                        : l10n.outOfStock,
                    style: TextStyle(
                      color:
                          product.stock > 0 ? AppColors.success : AppColors.error,
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                    ),
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),
                Text(
                  l10n.description,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  product.description,
                  style: Theme.of(context).textTheme.bodyLarge,
                ),
                const SizedBox(height: AppSpacing.lg),
                _buildReviewsSection(product, reviewProvider),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildReviewsSection(
      ProductModel product, ReviewProvider reviewProvider) {
    final l10n = AppLocalizations.of(context)!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              l10n.reviews,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            TextButton(
              onPressed: () => _openWriteReviewSheet(product),
              child: Text(l10n.writeAReview),
            ),
          ],
        ),
        if (reviewProvider.isLoading)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: AppSpacing.lg),
            child: Center(child: CircularProgressIndicator()),
          )
        else if (reviewProvider.reviews.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: AppSpacing.md),
            child: Text(
              l10n.noReviewsYet,
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(color: AppColors.grey),
            ),
          )
        else
          ...reviewProvider.reviews.map((review) => Padding(
                padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Text(
                          review.userName.isNotEmpty
                              ? review.userName
                              : l10n.anonymous,
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(width: AppSpacing.sm),
                        RatingStars(rating: review.rating, size: 14),
                      ],
                    ),
                    if (review.comment.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text(review.comment),
                    ],
                    const SizedBox(height: 4),
                    Text(
                      review.createdAt.toString().split(' ').first,
                      style: Theme.of(context)
                          .textTheme
                          .bodySmall
                          ?.copyWith(color: AppColors.grey),
                    ),
                    const Divider(height: AppSpacing.lg),
                  ],
                ),
              )),
      ],
    );
  }

  Widget _buildImageGallery(ProductModel product) {
    return Stack(
      children: [
        SizedBox(
          height: 320,
          width: double.infinity,
          child: product.images.isNotEmpty
              ? PageView.builder(
                  controller: _imageController,
                  itemCount: product.images.length,
                  onPageChanged: (i) => setState(() => _imagePage = i),
                  itemBuilder: (context, index) => CachedNetworkImage(
                    imageUrl: product.images[index],
                    fit: BoxFit.cover,
                    errorWidget: (_, __, ___) => const Center(
                      child: Icon(Icons.image_not_supported, size: 60),
                    ),
                  ),
                )
              : Container(
                  color: AppColors.background,
                  child: const Center(
                    child: Icon(Icons.image, size: 80, color: AppColors.grey),
                  ),
                ),
        ),
        if (product.images.length > 1)
          Positioned(
            bottom: AppSpacing.md,
            left: 0,
            right: 0,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(
                product.images.length,
                (i) => AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  margin: const EdgeInsets.symmetric(horizontal: 3),
                  width: _imagePage == i ? 18 : 6,
                  height: 6,
                  decoration: BoxDecoration(
                    color: _imagePage == i
                        ? AppColors.primary
                       : AppColors.secondary.withValues(alpha: 0.6),
                    borderRadius: BorderRadius.circular(AppRadius.chip),
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildStickyAddToCartBar(ProductModel product) {
    final l10n = AppLocalizations.of(context)!;
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg, vertical: AppSpacing.md),
        decoration: BoxDecoration(
          color: AppColors.secondary,
          boxShadow: [
            BoxShadow(
             color: Colors.black.withValues(alpha: 0.06),
              blurRadius: 8,
              offset: const Offset(0, -2),
            ),
          ],
        ),
        child: Row(
          children: [
            QuantityStepper(
              quantity: _quantity,
              max: product.stock > 0 ? product.stock : null,
              onChanged: (value) => setState(() => _quantity = value),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: PrimaryButton(
                label: product.stock == 0 ? l10n.outOfStock : l10n.addToCart,
                icon: product.stock == 0 ? null : Icons.shopping_cart_outlined,
                onPressed: product.stock == 0 ? null : () => _addToCart(product),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _WriteReviewSheet extends StatefulWidget {
  final String productId;
  final String uid;

  const _WriteReviewSheet({
    required this.productId,
    required this.uid,
  });

  @override
  State<_WriteReviewSheet> createState() => _WriteReviewSheetState();
}

class _WriteReviewSheetState extends State<_WriteReviewSheet> {
  double _rating = 5;
  final _commentController = TextEditingController();
  bool _submitting = false;

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _submitting = true);
    final l10n = AppLocalizations.of(context)!;

    final review = ReviewModel(
      id: '',
      productId: widget.productId,
      userId: widget.uid,
      userName: l10n.defaultUserName,
      rating: _rating,
      comment: _commentController.text.trim(),
      createdAt: DateTime.now(),
    );

    final success =
        await context.read<ReviewProvider>().submitReview(review);

    if (!mounted) return;

    setState(() => _submitting = false);

    if (success) {
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.failedToSubmitReview)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Padding(
      padding: EdgeInsets.only(
        left: AppSpacing.lg,
        right: AppSpacing.lg,
        top: AppSpacing.lg,
        bottom: MediaQuery.of(context).viewInsets.bottom + AppSpacing.lg,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            l10n.writeAReview,
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: AppSpacing.md),
          Center(
            child: RatingStars(
              rating: _rating,
              size: 32,
              onRatingChanged: (value) => setState(() => _rating = value),
            ),
          ),
          const SizedBox(height: AppSpacing.md),
          TextField(
            controller: _commentController,
            maxLines: 3,
            decoration: InputDecoration(
              hintText: l10n.shareYourThoughts,
              border: const OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),
          PrimaryButton(
            label: l10n.submitReview,
            isLoading: _submitting,
            onPressed: _submitting ? null : _submit,
          ),
        ],
      ),
    );
  }
}