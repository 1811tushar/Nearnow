import 'package:flutter/material.dart';
import '../models/order_model.dart';
import '../services/order_service.dart';

class OrderProvider extends ChangeNotifier {
  final OrderService _orderService;

  OrderProvider({OrderService? orderService})
      : _orderService = orderService ?? OrderService();

  List<OrderModel> _orders = [];
  bool _isLoading = false;
  String? _error;

  List<OrderModel> get orders => _orders;
  bool get isLoading => _isLoading;
  String? get error => _error;

  // uid kept (unused internally) for call-site compatibility — the
  // backend derives the caller from the JWT. items/totalAmount/
  // deliveryAddress are GONE from this signature on purpose: the
  // backend now computes both from its own server-side Cart state,
  // the client can no longer influence price or line-items at all.
  Future<bool> placeOrder({
    required String uid,
    required String addressId,
    required String paymentMethod,
  }) async {
    try {
      _isLoading = true;
      notifyListeners();

      final order = await _orderService.placeOrder(
        addressId: addressId,
        paymentMethod: paymentMethod,
      );
      _orders = [order, ..._orders];

      _error = null;
      return true;
    } catch (e) {
      _error = e.toString();
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> fetchOrders(String uid) async {
    try {
      _isLoading = true;
      notifyListeners();

      _orders = await _orderService.getOrdersByUser();
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Stream<OrderModel?> streamOrder(String orderId) {
    return _orderService.streamOrderById(orderId);
  }

  Future<bool> cancelOrder(String orderId) async {
    try {
      final updated = await _orderService.cancelOrder(orderId);
      final index = _orders.indexWhere((o) => o.id == orderId);
      if (index != -1) _orders[index] = updated;
      notifyListeners();
      return true;
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      return false;
    }
  }
}