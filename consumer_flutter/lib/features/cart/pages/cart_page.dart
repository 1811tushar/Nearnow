import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:provider/provider.dart';
import 'package:razorpay_flutter/razorpay_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';
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

  // One Razorpay instance per page — event callbacks below are wired
  // once in initState and torn down in dispose, standard razorpay_flutter
  // usage pattern.
  late final Razorpay _razorpay;
  AddressModel? _pendingAddressForRazorpay;
  String? _uidForRazorpay;

  @override
  void initState() {
    super.initState();
    _razorpay = Razorpay();
    _razorpay.on(Razorpay.EVENT_PAYMENT_SUCCESS, _handleRazorpaySuccess);
    _razorpay.on(Razorpay.EVENT_PAYMENT_ERROR, _handleRazorpayError);
    _razorpay.on(Razorpay.EVENT_EXTERNAL_WALLET, _handleRazorpayExternalWallet);

    WidgetsBinding.instance.addPostFrameCallback((_) {
      final uid = context.read<AuthProvider>().user?.uid;
      if (uid != null) {
        context.read<CartProvider>().fetchCart(uid);
        context.read<AddressProvider>().fetchAddresses(uid);
      }
    });
  }

  @override
  void dispose() {
    _razorpay.clear();
    super.dispose();
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
      await PaymentService().verifyMockPayment(
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

  /// Real Razorpay flow:
  /// 1. Ask our backend to create a real Razorpay order (test mode) —
  ///    returns Razorpay's order id + the public key id + amount.
  /// 2. Open Razorpay's own checkout sheet with those values. If the user
  ///    picks UPI there, Razorpay hands off to whichever UPI app (Google
  ///    Pay, PhonePe, ...) is installed to actually authorize the payment.
  /// 3. Razorpay calls us back (via the event handlers below) with a
  ///    payment id + signature, which we send to our backend to verify
  ///    and place the order — the order is never placed on the strength
  ///    of the app alone, only after the backend confirms the signature.
  Future<void> _startRazorpayCheckout({required String uid, required AddressModel deliveryAddress}) async {
    setState(() => _isPlacingOrder = true);
    try {
      final payment = await PaymentService().createOrder();
      if (payment.mode != 'RAZORPAY' || payment.razorpayKeyId == null) {
        // Server isn't configured for real Razorpay payments right now
        // (payment.mode isn't RAZORPAY, or keys are missing) — fail
        // clearly instead of silently trying to open a broken checkout.
        throw Exception('Online payment is not available right now. Please try Cash on Delivery.');
      }

      _pendingAddressForRazorpay = deliveryAddress;
      _uidForRazorpay = uid;
      // Razorpay's checkout runs in a SEPARATE native Android Activity,
      // not a Flutter widget — that Activity switch can trigger Flutter's
      // engine to pause/detach in ways that occasionally reset this
      // State object's fields to null before the success callback fires
      // (observed as a silent no-op: payment succeeds, but the order
      // never gets placed because uid/address had already gone null).
      // SharedPreferences survives that switch, so _handleRazorpaySuccess
      // below falls back to it if the in-memory fields are gone.
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('pending_razorpay_uid', uid);
      await prefs.setString('pending_razorpay_address_id', deliveryAddress.id.toString());

      final options = {
        'key': payment.razorpayKeyId,
        'amount': (payment.amount * 100).round(), // paise, matches backend's calculation
        'currency': payment.currency,
        'name': 'NearNow',
        'description': 'Order payment',
        'order_id': payment.paymentReference, // Razorpay's real order id
        // Nudges the checkout sheet to show UPI (and therefore the
        // Google Pay / PhonePe hand-off) as a prominent option, rather
        // than defaulting straight to cards.
        'method': {'upi': true, 'card': true, 'netbanking': true, 'wallet': true},
      };

      _razorpay.open(options);
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
      if (mounted) setState(() => _isPlacingOrder = false);
    }
  }

  Future<void> _handleRazorpaySuccess(PaymentSuccessResponse response) async {
    String? uid = _uidForRazorpay;
    String? addressId = _pendingAddressForRazorpay?.id;
    if (uid == null || addressId == null) {
      // In-memory fields were lost (see the comment where they're set,
      // just before _razorpay.open) — recover from SharedPreferences
      // instead of silently doing nothing.
      final prefs = await SharedPreferences.getInstance();
      uid ??= prefs.getString('pending_razorpay_uid');
      addressId ??= prefs.getString('pending_razorpay_address_id');
    }
    if (uid == null || addressId == null) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(
              'Payment received, but we lost track of your delivery address. '
              'Please contact support with your payment ID: ${response.paymentId ?? "unknown"}')),
        );
      }
      return;
    }

    try {
      await PaymentService().verifyRazorpayPayment(
        razorpayOrderId: response.orderId ?? '',
        razorpayPaymentId: response.paymentId ?? '',
        razorpaySignature: response.signature ?? '',
        addressId: addressId,
      );
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('pending_razorpay_uid');
      await prefs.remove('pending_razorpay_address_id');
      if (!mounted) return;
      await context.read<CartProvider>().clearCart(uid);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Payment successful. Order placed.')),
      );
      AppShell.of(context)?.switchToTab(2);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Payment received but order placement failed: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isPlacingOrder = false);
    }
  }

  void _handleRazorpayError(PaymentFailureResponse response) {
    if (!mounted) return;
    setState(() => _isPlacingOrder = false);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Payment failed: ${response.message ?? "Please try again."}')),
    );
  }

  void _handleRazorpayExternalWallet(ExternalWalletResponse response) {
    if (!mounted) return;
    setState(() => _isPlacingOrder = false);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Redirected to ${response.walletName ?? "external wallet"}.')),
    );
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
    const methods = ['Cash on Delivery', 'Razorpay', 'Mock Online Payment'];
    String labelFor(String method) {
      switch (method) {
        case 'Cash on Delivery':
          return l10n.cashOnDelivery;
        case 'Razorpay':
          return 'UPI / Cards / Netbanking';
        case 'Mock Online Payment':
          return 'Mock online payment';
        case 'UPI':
          return l10n.upi;
        default:
          return l10n.card;
      }
    }

    String subtitleFor(String method) {
      switch (method) {
        case 'Cash on Delivery':
          return l10n.payWhenOrderArrives;
        case 'Razorpay':
          return 'Pay via Google Pay, PhonePe, card, or netbanking';
        default:
          return 'Development-only payment; no real money is charged';
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
            subtitle: Text(subtitleFor(method)),
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
      case 'Razorpay':
        return 'UPI / Cards / Netbanking';
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
                          } else if (_paymentMethod == 'Razorpay') {
                            _startRazorpayCheckout(uid: uid, deliveryAddress: selectedAddress);
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