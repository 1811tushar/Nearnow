import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:provider/provider.dart';
import '../providers/cart_provider.dart';
import '../../auth/providers/auth_provider.dart';
import '../../../core/widgets/loading_widget.dart';
import '../../../core/widgets/empty_state_widget.dart';
import '../../../core/widgets/primary_button.dart';
import '../../../core/widgets/quantity_stepper.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_radius.dart';
import '../../../core/constants/app_spacing.dart';
import '../../orders/providers/order_provider.dart';
import '../../payment/services/payment_service.dart';
import '../../address/providers/address_provider.dart';
import '../../address/pages/address_list_page.dart';
import '../../address/models/address_model.dart';
import '../../../core/pages/app_shell.dart';
import '../../../l10n/app_localizations.dart';
class CartPage extends StatefulWidget {
  const CartPage({super.key});

  @override
  State<CartPage> createState() => _CartPageState();
}

class _CartPageState extends State<CartPage> {
  bool _isPlacingOrder = false;
  String _paymentMethod = 'Cash on Delivery';


  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final uid = context.read<AuthProvider>().user?.uid;
      if (uid != null) {
        context.read<CartProvider>().fetchCart(uid);
        context.read<AddressProvider>().fetchAddresses(uid);
      }
    });
  }

  Future<void> _startMockCheckout({required String uid, required AddressModel deliveryAddress}) async {
    setState(() => _isPlacingOrder = true);
    try {
      final payment = await PaymentService().createOrder();
      if (!mounted) return;
      setState(() => _isPlacingOrder = false);
      final outcome = await showDialog<String>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Mock online payment'),
          content: Text('Development-only payment for ${payment.currency} ${payment.amount.toStringAsFixed(2)}. No real money is charged.'),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context, 'FAILED'), child: const Text('Simulate failure')),
            FilledButton(onPressed: () => Navigator.pop(context, 'SUCCESS'), child: const Text('Simulate success')),
          ],
        ),
      );
      if (outcome == null) return;
      setState(() => _isPlacingOrder = true);
      await PaymentService().verifyPayment(
        paymentReference: payment.paymentReference,
        outcome: outcome,
        addressId: deliveryAddress.id,
      );
      if (!mounted) return;
      await context.read<CartProvider>().clearCart(uid);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Mock payment successful. Order placed.')));
      AppShell.of(context)?.switchToTab(2);
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      if (mounted) setState(() => _isPlacingOrder = false);
    }
  }

  Future<void> _placeOrder({
    required String uid,
    required CartProvider cartProvider,
    required AddressModel deliveryAddress,
  }) async {
    final l10n = AppLocalizations.of(context)!;
    final orderProvider = context.read<OrderProvider>();
    setState(() => _isPlacingOrder = true);

    final success = await orderProvider.placeOrder(
          uid: uid,
          addressId: deliveryAddress.id,
          paymentMethod: _paymentMethod,
        );

    if (!mounted) return;
    setState(() => _isPlacingOrder = false);

    if (success) {
      await cartProvider.clearCart(uid);
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.orderPlacedSuccessfully),
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppRadius.button),
          ),
        ),
      );

      AppShell.of(context)?.switchToTab(2);
    } else {
      final message = orderProvider.error ?? l10n.failedToPlaceOrder;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message)),
      );
    }
  }

  Future<void> _selectPaymentMethod() async {
    final l10n = AppLocalizations.of(context)!;
    const methods = ['Cash on Delivery', 'Mock Online Payment'];
    String labelFor(String method) {
      switch (method) {
        case 'Cash on Delivery':
          return l10n.cashOnDelivery;
        case 'Mock Online Payment':
          return 'Mock online payment';
        case 'UPI':
          return l10n.upi;
        default:
          return l10n.card;
      }
    }

    final selected = await showModalBottomSheet<String>(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppRadius.sheet)),
      ),
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: methods.map((method) => RadioListTile<String>(
            value: method,
            groupValue: _paymentMethod,
            title: Text(labelFor(method)),
            subtitle: Text(method == 'Cash on Delivery' ? l10n.payWhenOrderArrives : 'Development-only payment; no real money is charged'),
            onChanged: (value) => Navigator.pop(context, value),
          )).toList(),
        ),
      ),
    );
    if (selected != null && mounted) setState(() => _paymentMethod = selected);
  }

  String _paymentMethodLabel(AppLocalizations l10n) {
    switch (_paymentMethod) {
      case 'Cash on Delivery':
        return l10n.cashOnDelivery;
      case 'Mock Online Payment':
        return 'Mock online payment';
      case 'UPI':
        return l10n.upi;
      default:
        return l10n.card;
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final cartProvider = context.watch<CartProvider>();
    final addressProvider = context.watch<AddressProvider>();
    final uid = context.read<AuthProvider>().user?.uid;
    final selectedAddress = addressProvider.selectedAddress;

    final subtotal = cartProvider.subtotal;
    final deliveryFee = cartProvider.deliveryFee;
    final total = cartProvider.grandTotal;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.myCart)),
      body: cartProvider.isLoading
          ? const LoadingWidget()
          : cartProvider.error != null
              ? EmptyStateWidget(
                  icon: Icons.error_outline,
                  title: l10n.somethingWentWrong,
                  subtitle: cartProvider.error,
                  actionLabel: l10n.retry,
                  onAction: () => cartProvider.fetchCart(uid ?? ''),
                )
              : cartProvider.items.isEmpty
              ? EmptyStateWidget(
                  icon: Icons.shopping_cart_outlined,
                  title: l10n.cartEmpty,
                  subtitle: l10n.addItemsToGetStarted,
                  actionLabel: l10n.startShopping,
                  onAction: () {
  AppShell.of(context)?.switchToTab(0);
},
                 
                )
              : ListView(
                  padding: const EdgeInsets.all(AppSpacing.lg),
                  children: [
                    // Deliver-to card
                    Container(
                      padding: const EdgeInsets.all(AppSpacing.md),
                      decoration: BoxDecoration(
                        color: AppColors.secondary,
                        borderRadius: BorderRadius.circular(AppRadius.card),
                        border: Border.all(
                            color: AppColors.grey.withValues(alpha: 0.2)),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.location_on_outlined,
                              color: AppColors.primary),
                          const SizedBox(width: AppSpacing.sm),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  selectedAddress != null
                                      ? l10n.deliverTo(selectedAddress.label)
                                      : l10n.noDeliveryAddressSelected,
                                  style: Theme.of(context)
                                      .textTheme
                                      .titleMedium,
                                ),
                                if (selectedAddress != null)
                                  Text(
                                    "${selectedAddress.addressLine}, ${selectedAddress.city} - ${selectedAddress.pincode}",
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodyMedium
                                        ?.copyWith(color: AppColors.grey),
                                  ),
                              ],
                            ),
                          ),
                          TextButton(
                            onPressed: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(
                                    builder: (_) => const AddressListPage()),
                              );
                            },
                            child: Text(l10n.change),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: AppSpacing.lg),

                    // Cart items
                    ...cartProvider.items.map((item) {
                      return Container(
                        margin: const EdgeInsets.only(bottom: AppSpacing.md),
                        padding: const EdgeInsets.all(AppSpacing.sm),
                        decoration: BoxDecoration(
                          color: AppColors.secondary,
                          borderRadius: BorderRadius.circular(AppRadius.card),
                        ),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            ClipRRect(
                              borderRadius:
                                  BorderRadius.circular(AppRadius.button),
                              child: item.image.isNotEmpty
                                  ? CachedNetworkImage(
                                      imageUrl: item.image,
                                      width: 56,
                                      height: 56,
                                      fit: BoxFit.cover,
                                      errorWidget: (_, __, ___) => Container(
                                        width: 56,
                                        height: 56,
                                        color: AppColors.background,
                                        child: const Icon(
                                            Icons.image_not_supported),
                                      ),
                                    )
                                  : Container(
                                      width: 56,
                                      height: 56,
                                      color: AppColors.background,
                                      child: const Icon(Icons.image),
                                    ),
                            ),
                            const SizedBox(width: AppSpacing.md),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    item.name,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodyLarge
                                        ?.copyWith(fontWeight: FontWeight.w600),
                                  ),
                                  const SizedBox(height: 2),
                                  Text(
                                    "${item.unit} • ₹${item.price}",
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodyMedium
                                        ?.copyWith(color: AppColors.grey),
                                  ),
                                  const SizedBox(height: AppSpacing.sm),
                                  Row(
                                    mainAxisAlignment:
                                        MainAxisAlignment.spaceBetween,
                                    children: [
                                      QuantityStepper(
                                        quantity: item.quantity,
                                        iconSize: 14,
                                        onChanged: (value) {
                                          if (uid == null) return;
                                          if (value < 1) {
                                            cartProvider.removeFromCart(
                                                uid, item.id);
                                          } else {
                                            cartProvider.updateQuantity(
                                                uid, item.id, value);
                                          }
                                        },
                                      ),
                                      Text(
                                        "₹${item.itemTotal.toStringAsFixed(2)}",
                                        style: Theme.of(context)
                                            .textTheme
                                            .bodyLarge
                                            ?.copyWith(
                                                fontWeight: FontWeight.bold),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                            IconButton(
                              icon: const Icon(Icons.delete_outline,
                                  color: AppColors.error),
                              onPressed: uid == null
                                  ? null
                                  : () => cartProvider.removeFromCart(
                                      uid, item.id),
                            ),
                          ],
                        ),
                      );
                    }),

                    const SizedBox(height: AppSpacing.md),

                    ListTile(
                      contentPadding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
                      leading: const Icon(Icons.payment_outlined),
                      title: Text(l10n.paymentMethod),
                      subtitle: Text(_paymentMethodLabel(l10n)),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: _selectPaymentMethod,
                    ),
                    const SizedBox(height: AppSpacing.md),

                    // Price breakdown
                    Container(
                      padding: const EdgeInsets.all(AppSpacing.md),
                      decoration: BoxDecoration(
                        color: AppColors.secondary,
                        borderRadius: BorderRadius.circular(AppRadius.card),
                      ),
                      child: Column(
                        children: [
                          _priceRow(context, l10n.subtotal,
                              "₹${subtotal.toStringAsFixed(2)}"),
                          const SizedBox(height: AppSpacing.xs),
                          _priceRow(
                            context,
                            l10n.deliveryFee,
                            deliveryFee == 0
                                ? l10n.free
                                : "₹${deliveryFee.toStringAsFixed(2)}",
                            valueColor:
                                deliveryFee == 0 ? AppColors.success : null,
                          ),
                          const Divider(height: AppSpacing.lg),
                          _priceRow(
                            context,
                            l10n.total,
                            "₹${total.toStringAsFixed(2)}",
                            isBold: true,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
      bottomNavigationBar: (cartProvider.items.isEmpty || cartProvider.isLoading)
          ? null
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(AppSpacing.lg),
                child: PrimaryButton(
                  label: l10n.placeOrderWithTotal(total.toStringAsFixed(2)),
                  isLoading: _isPlacingOrder,
onPressed: (uid == null || selectedAddress == null)
                      ? null
                      : () {
                          if (_paymentMethod == 'Mock Online Payment') {
                            _startMockCheckout(uid: uid, deliveryAddress: selectedAddress);
                          } else {
                            _placeOrder(
                              uid: uid,
                              cartProvider: cartProvider,
                              deliveryAddress: selectedAddress,
                            );
                          }
                        },
                ),
              ),
            ),
    );
  }

  Widget _priceRow(
    BuildContext context,
    String label,
    String value, {
    bool isBold = false,
    Color? valueColor,
  }) {
    final style = isBold
        ? Theme.of(context)
            .textTheme
            .titleMedium
            ?.copyWith(fontWeight: FontWeight.bold)
        : Theme.of(context).textTheme.bodyLarge;

    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: style),
        Text(value, style: style?.copyWith(color: valueColor)),
      ],
    );
  }
}
