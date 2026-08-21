import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/order_model.dart';
import '../providers/order_provider.dart';
import '../../../core/widgets/loading_widget.dart';
import '../../cart/models/cart_item_model.dart';
import '../../cart/providers/cart_provider.dart';
import '../../auth/providers/auth_provider.dart';
import '../../../core/pages/app_shell.dart';
import '../../../l10n/app_localizations.dart';

class OrderDetailPage extends StatelessWidget {
  final String orderId;

  const OrderDetailPage({super.key, required this.orderId});

  static const _statusSteps = ['placed', 'packed', 'out_for_delivery', 'delivered'];

  String _statusLabel(AppLocalizations l10n, String status) {
    switch (status) {
      case 'placed':
        return l10n.statusPlaced;
      case 'packed':
        return l10n.statusPacked;
      case 'out_for_delivery':
        return l10n.statusOutForDelivery;
      default:
        return l10n.statusDelivered;
    }
  }

  int _currentStepIndex(String status) {
    final index = _statusSteps.indexOf(status);
    return index == -1 ? 0 : index;
  }

  Future<void> _reorder(BuildContext context, OrderModel order) async {
    final uid = context.read<AuthProvider>().user?.uid;
    if (uid == null) return;

    final cartProvider = context.read<CartProvider>();
    final l10n = AppLocalizations.of(context)!;

    // One batched write for every item instead of the previous approach
    // — N sequential `addToCart()` calls, each of which itself opened
    // its own transaction and triggered a separate cart re-fetch. For a
    // 10-item order that used to mean 10 round-trips in a row before the
    // person saw anything happen; now it's one.
    final cartItems = order.items
        .map((item) => CartItemModel(
              id: '',
              productId: item.productId,
              name: item.name,
              image: item.image,
              price: item.price,
              unit: item.unit,
              quantity: item.quantity,
            ))
        .toList();

    await cartProvider.addMultipleToCart(uid, cartItems);

    if (!context.mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(l10n.itemsAddedToCart)),
    );

    Navigator.of(context).popUntil((route) => route.isFirst);
    AppShell.of(context)?.switchToTab(1);
  }

  Future<void> _confirmCancel(BuildContext context, String orderId) async {
    final l10n = AppLocalizations.of(context)!;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(l10n.cancelOrderConfirmTitle),
        content: Text(l10n.cancelOrderConfirmBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: Text(l10n.keepOrder),
          ),
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: Text(l10n.yesCancel,
                style: const TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirmed != true || !context.mounted) return;

    final success =
        await context.read<OrderProvider>().cancelOrder(orderId);

    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
            success ? l10n.orderCancelled : l10n.failedToCancelOrder),
      ),
    );
  }

  String _displayOrderId(String id) =>
      id.length <= 8 ? id : id.substring(0, 8);

  @override
  Widget build(BuildContext context) {
    final orderProvider = context.read<OrderProvider>();
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.orderNumber(_displayOrderId(orderId)))),
      body: StreamBuilder<OrderModel?>(
        stream: orderProvider.streamOrder(orderId),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const LoadingWidget();
          }

          if (!snapshot.hasData || snapshot.data == null) {
            return Center(child: Text(l10n.orderNotFound));
          }

          final order = snapshot.data!;

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              if (order.status != 'cancelled')
                Row(
                  children: List.generate(_statusSteps.length, (index) {
                    final isActive = index <= _currentStepIndex(order.status);
                    return Expanded(
                      child: Column(
                        children: [
                          CircleAvatar(
                            radius: 14,
                            backgroundColor:
                                isActive ? Colors.green : Colors.grey.shade300,
                            child: Icon(
                              Icons.check,
                              size: 16,
                              color: isActive ? Colors.white : Colors.grey,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            _statusLabel(l10n, _statusSteps[index]),
                            textAlign: TextAlign.center,
                            style: const TextStyle(fontSize: 9),
                          ),
                        ],
                      ),
                    );
                  }),
                )
              else
                Center(
                  child: Text(
                    l10n.statusCancelled,
                    style: const TextStyle(
                        color: Colors.red, fontWeight: FontWeight.bold),
                  ),
                ),
              const SizedBox(height: 24),
              Text(
                l10n.placedOn(order.createdAt.toString().split(' ').first),
                style: const TextStyle(color: Colors.grey),
              ),
              const SizedBox(height: 8),
              Text(
                l10n.deliveringToFull(order.deliveryAddress.fullName, order.deliveryAddress.addressLine, order.deliveryAddress.city, order.deliveryAddress.pincode),
                style: const TextStyle(color: Colors.grey),
              ),
              const Divider(height: 32),
              Text(
                l10n.items,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
              const SizedBox(height: 12),
              ...order.items.map((item) {
                return ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: item.image.isNotEmpty
                      ? CachedNetworkImage(
                          imageUrl: item.image,
                          width: 50,
                          height: 50,
                          fit: BoxFit.cover,
                          errorWidget: (_, __, ___) =>
                              const Icon(Icons.image_not_supported),
                        )
                      : const Icon(Icons.image),
                  title: Text(item.name),
                  subtitle: Text("${item.unit} • Qty: ${item.quantity}"),
                  trailing: Text("₹${item.price.toStringAsFixed(0)}"),
                );
              }),
              const Divider(height: 32),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    l10n.total,
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                  Text(
                    "₹${order.totalAmount.toStringAsFixed(2)}",
                    style: const TextStyle(
                        fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: () => _reorder(context, order),
                  icon: const Icon(Icons.replay),
                  label: Text(l10n.reorder),
                ),
              ),
              if (order.isCancellable) ...[
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    style: OutlinedButton.styleFrom(foregroundColor: Colors.red),
                    onPressed: () => _confirmCancel(context, order.id),
                    icon: const Icon(Icons.cancel_outlined),
                    label: Text(l10n.cancelOrder),
                  ),
                ),
              ],
            ],
          );
        },
      ),
    );
  }
}
