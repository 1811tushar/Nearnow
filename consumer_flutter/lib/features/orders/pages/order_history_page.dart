
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/order_provider.dart';
import '../../auth/providers/auth_provider.dart';
import '../../../core/widgets/loading_widget.dart';
import '../../../core/widgets/empty_state_widget.dart';
import 'order_detail_page.dart';
import '../../../l10n/app_localizations.dart';

class OrderHistoryPage extends StatefulWidget {
  const OrderHistoryPage({super.key});

  @override
  State<OrderHistoryPage> createState() => _OrderHistoryPageState();
}

class _OrderHistoryPageState extends State<OrderHistoryPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final uid = context.read<AuthProvider>().user?.uid;
      if (uid != null) {
        context.read<OrderProvider>().fetchOrders(uid);
      }
    });
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'delivered':
        return Colors.green;
      case 'out_for_delivery':
        return Colors.blue;
      case 'packed':
        return Colors.orange;
      case 'cancelled':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  String _statusLabel(AppLocalizations l10n, String status) {
    switch (status) {
      case 'delivered':
        return l10n.statusDelivered;
      case 'out_for_delivery':
        return l10n.statusOutForDelivery;
      case 'packed':
        return l10n.statusPacked;
      case 'cancelled':
        return l10n.statusCancelled;
      default:
        return l10n.statusPlaced;
    }
  }

  @override
  Widget build(BuildContext context) {
    final orderProvider = context.watch<OrderProvider>();
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.orders)),
      body: orderProvider.isLoading
          ? const LoadingWidget()
          : orderProvider.error != null
              ? EmptyStateWidget(
                  icon: Icons.error_outline,
                  title: l10n.somethingWentWrong,
                  subtitle: orderProvider.error,
                  actionLabel: l10n.retry,
                  onAction: () {
                    final uid = context.read<AuthProvider>().user?.uid;
                    if (uid != null) orderProvider.fetchOrders(uid);
                  },
                )
              : orderProvider.orders.isEmpty
              ? EmptyStateWidget(
                  icon: Icons.receipt_long_outlined,
                  title: l10n.noOrdersYet,
                )
              : RefreshIndicator(
                  onRefresh: () {
                    final uid = context.read<AuthProvider>().user?.uid;
                    return uid != null
                        ? orderProvider.fetchOrders(uid)
                        : Future.value();
                  },
                  child: ListView.builder(
                  padding: const EdgeInsets.all(12),
                  itemCount: orderProvider.orders.length,
                  itemBuilder: (context, index) {
                    final order = orderProvider.orders[index];
                    return Card(
                      child: ListTile(
                        title: Text(
                            l10n.orderNumber(order.id.length <= 8 ? order.id : order.id.substring(0, 8))),
                        subtitle: Text(
                          "${order.createdAt.toString().split(' ').first} • ₹${order.totalAmount.toStringAsFixed(2)}",
                        ),
                        trailing: Chip(
                          label: Text(
                            _statusLabel(l10n, order.status),
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 11,
                            ),
                          ),
                          backgroundColor: _statusColor(order.status),
                        ),
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) =>
                                  OrderDetailPage(orderId: order.id),
                            ),
                          );
                        },
                      ),
                    );
                  },
                )),
    );
  }
}