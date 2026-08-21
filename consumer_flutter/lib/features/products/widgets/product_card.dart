import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/product_model.dart';
import '../pages/product_detail_page.dart';
import '../../wishlist/providers/wishlist_provider.dart';
import '../../auth/providers/auth_provider.dart';
import '../../cart/providers/cart_provider.dart';
import '../../cart/models/cart_item_model.dart';
import '../../address/providers/address_provider.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/rating_stars.dart';
import '../../../core/utils/delivery_estimate.dart';
import '../../../l10n/app_localizations.dart';

class ProductCard extends StatelessWidget {
  final ProductModel product;
  final double? width;

  const ProductCard({super.key, required this.product, this.width});

  String _formatReviewCount(int count) {
    if (count >= 100000) {
      return "${(count / 100000).toStringAsFixed(1)} lac";
    } else if (count >= 1000) {
      return "${(count / 1000).toStringAsFixed(1)}k";
    }
    return "$count";
  }

  void _addToCart(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final uid = context.read<AuthProvider>().user?.uid;
    if (uid == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.pleaseLogInToContinue)),
      );
      return;
    }

    final cartItem = CartItemModel(
      id: '',
      productId: product.id,
      name: product.name,
      image: product.images.isNotEmpty ? product.images.first : '',
      price: product.effectivePrice,
      unit: product.unit,
      quantity: 1,
    );

    context.read<CartProvider>().addToCart(uid, cartItem);

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(l10n.addedToCart(product.name)),
        behavior: SnackBarBehavior.floating,
        duration: const Duration(seconds: 1),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final selectedAddress = context.watch<AddressProvider>().selectedAddress;
    final l10n = AppLocalizations.of(context)!;

    final hasDiscount = product.discountPercent > 0;

    final card = GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => ProductDetailPage(productId: product.id),
          ),
        );
      },
      child: Container(
        decoration: BoxDecoration(
          color: AppColors.secondary,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: AppColors.background, width: 1),
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Image + wishlist heart + unit/ADD overlay
            AspectRatio(
              aspectRatio: 1,
              child: Stack(
                children: [
                  Positioned.fill(
                    child: product.images.isNotEmpty
                        ? CachedNetworkImage(
                            imageUrl: product.images.first,
                            fit: BoxFit.cover,
                            placeholder: (_, __) => Container(
                              color: AppColors.background,
                            ),
                            errorWidget: (_, __, ___) => Container(
                              color: AppColors.background,
                              child: const Icon(Icons.image_not_supported,
                                  color: AppColors.grey),
                            ),
                          )
                        : Container(
                            color: AppColors.background,
                            child: const Icon(Icons.image,
                                size: 40, color: AppColors.grey),
                          ),
                  ),
                  if (hasDiscount)
                    Positioned(
                      top: 6,
                      left: 6,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: AppColors.primary,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          "${product.discountPercent}% OFF",
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 9,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                  Positioned(
                    top: 4,
                    right: 4,
                    child: Consumer<WishlistProvider>(
                      builder: (context, wishlistProvider, _) {
                        final isSaved =
                            wishlistProvider.isInWishlist(product.id);
                        return GestureDetector(
                          onTap: () {
                            final uid =
                                context.read<AuthProvider>().user?.uid;
                            if (uid != null) {
                              wishlistProvider.toggleWishlist(
                                  uid, product.id);
                            } else {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                    content:
                                        Text(l10n.pleaseLogInToContinue)),
                              );
                            }
                          },
                          child: Icon(
                            isSaved ? Icons.favorite : Icons.favorite_border,
                            size: 16,
                            color: isSaved ? AppColors.error : AppColors.grey,
                          ),
                        );
                      },
                    ),
                  ),
                  // Unit pill + ADD button, bottom-right overlapping the image
                  Positioned(
                    right: 6,
                    bottom: -14,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: AppColors.secondary,
                            border: Border.all(color: AppColors.background),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text(
                            product.unit,
                            style: const TextStyle(
                                fontSize: 9, color: AppColors.grey),
                          ),
                        ),
                        const SizedBox(height: 2),
                        GestureDetector(
                          onTap: product.stock == 0
                              ? null
                              : () => _addToCart(context),
                          child: Container(
                            width: 56,
                            padding: const EdgeInsets.symmetric(vertical: 4),
                            decoration: BoxDecoration(
                              color: product.stock == 0
                                  ? AppColors.grey
                                  : AppColors.secondary,
                              border: Border.all(
                                color: product.stock == 0
                                    ? AppColors.grey
                                    : AppColors.success,
                              ),
                              borderRadius: BorderRadius.circular(6),
                            ),
                            alignment: Alignment.center,
                            child: Text(
                              product.stock == 0 ? "—" : l10n.addShort,
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.bold,
                                color: product.stock == 0
                                    ? Colors.white
                                    : AppColors.success,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(
                  AppSpacing.sm, 18, AppSpacing.sm, AppSpacing.sm),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(
                        "₹${product.effectivePrice.toStringAsFixed(0)}",
                        style: Theme.of(context)
                            .textTheme
                            .bodyMedium
                            ?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      if (hasDiscount) ...[
                        const SizedBox(width: 4),
                        Text(
                          "₹${product.price.toStringAsFixed(0)}",
                          style: const TextStyle(
                            fontSize: 11,
                            decoration: TextDecoration.lineThrough,
                            color: AppColors.grey,
                          ),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    product.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 3),
                  Row(
                    children: [
                      RatingStars(rating: product.rating, size: 11),
                      const SizedBox(width: 3),
                      Text(
                        _formatReviewCount(product.reviewCount),
                        style: const TextStyle(
                            fontSize: 10, color: AppColors.grey),
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Row(
                    children: [
                      const Icon(Icons.timer_outlined,
                          size: 11, color: AppColors.grey),
                      const SizedBox(width: 3),
                      Text(
                        DeliveryEstimate.estimateFor(
                          latitude: selectedAddress?.latitude,
                          longitude: selectedAddress?.longitude,
                        ),
                        style: const TextStyle(
                            fontSize: 10, color: AppColors.grey),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );

    return width != null ? SizedBox(width: width, child: card) : card;
  }
}