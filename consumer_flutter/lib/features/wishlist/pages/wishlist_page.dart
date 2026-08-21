import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../providers/wishlist_provider.dart';
import '../../products/models/product_model.dart';
import '../../products/providers/product_provider.dart';
import '../../../core/widgets/loading_widget.dart';
import '../../../core/widgets/empty_state_widget.dart';
import '../../../l10n/app_localizations.dart';

class WishlistPage extends StatefulWidget {
  final String uid;

  const WishlistPage({super.key, required this.uid});

  @override
  State<WishlistPage> createState() => _WishlistPageState();
}

class _WishlistPageState extends State<WishlistPage> {
  List<ProductModel> _products = [];
  bool _loadingProducts = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadWishlistProducts();
    });
  }

  Future<void> _loadWishlistProducts() async {
    setState(() {
      _loadingProducts = true;
      _error = null;
    });

    try {
      final wishlistProvider = context.read<WishlistProvider>();
      final productProvider = context.read<ProductProvider>();
      await wishlistProvider.fetchWishlist(widget.uid);

      final ids = wishlistProvider.wishlistIds;

      // One batched query instead of one sequential await per item — see
      // ProductService.getProductsByIds for why.
      final loaded = await productProvider.fetchProductsByIds(ids);

      // Any ID with no matching product means the underlying product was
      // deleted after being wishlisted — clean those orphaned IDs out of
      // the wishlist now rather than letting them sit there forever.
      final foundIds = loaded.map((p) => p.id).toSet();
      final orphanedIds = ids.where((id) => !foundIds.contains(id)).toList();
      for (final orphanId in orphanedIds) {
        // toggleWishlist removes it, since it's currently present.
        await wishlistProvider.toggleWishlist(widget.uid, orphanId);
      }

      if (!mounted) return;
      setState(() {
        _products = loaded;
        _loadingProducts = false;
      });
    } catch (e) {
      // Previously an exception mid-load left _loadingProducts stuck
      // true forever (an infinite spinner) — now it's always resolved,
      // success or failure.
      if (!mounted) return;
      setState(() {
        _error = e.toString();
        _loadingProducts = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final wishlistProvider = context.watch<WishlistProvider>();

    return Scaffold(
      appBar: AppBar(title: Text(l10n.myWishlist)),
      body: _loadingProducts
          ? const LoadingWidget()
          : _error != null
              ? EmptyStateWidget(
                  icon: Icons.error_outline,
                  title: l10n.somethingWentWrong,
                  subtitle: _error,
                  actionLabel: l10n.retry,
                  onAction: _loadWishlistProducts,
                )
              : _products.isEmpty
                  ? EmptyStateWidget(
                      icon: Icons.favorite_border,
                      title: l10n.wishlistEmpty,
                    )
                  : RefreshIndicator(
                      onRefresh: _loadWishlistProducts,
                      child: GridView.builder(
                      padding: const EdgeInsets.all(12),
                      gridDelegate:
                          const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 2,
                        childAspectRatio: 0.7,
                        crossAxisSpacing: 12,
                        mainAxisSpacing: 12,
                      ),
                      itemCount: _products.length,
                      itemBuilder: (context, index) {
                        final product = _products[index];
                        return Card(
                          clipBehavior: Clip.antiAlias,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(
                                child: Stack(
                                  children: [
                                    Positioned.fill(
                                      child: product.images.isNotEmpty
                                          ? CachedNetworkImage(
                                              imageUrl: product.images.first,
                                              fit: BoxFit.cover,
                                              errorWidget: (_, __, ___) =>
                                                  const Icon(Icons
                                                      .image_not_supported),
                                            )
                                          : const Icon(
                                              Icons.image_not_supported),
                                    ),
                                    Positioned(
                                      top: 4,
                                      right: 4,
                                      child: IconButton(
                                        icon: const Icon(
                                          Icons.favorite,
                                          color: Colors.red,
                                        ),
                                        onPressed: () async {
                                          // Optimistic removal from this
                                          // page's own list; if the
                                          // underlying write fails,
                                          // WishlistProvider now rolls its
                                          // own state back and this page
                                          // will simply reflect whatever
                                          // is true after the next fetch.
                                          setState(() {
                                            _products.removeWhere(
                                              (p) => p.id == product.id,
                                            );
                                          });
                                          await wishlistProvider
                                              .toggleWishlist(
                                            widget.uid,
                                            product.id,
                                          );
                                        },
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Padding(
                                padding: const EdgeInsets.all(8),
                                child: Column(
                                  crossAxisAlignment:
                                      CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      product.name,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    Text(
                                      "₹${product.effectivePrice.toStringAsFixed(0)}",
                                      style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        );
                      },
                    )),
    );
  }
}
