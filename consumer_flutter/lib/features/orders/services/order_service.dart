import 'dart:async';
import '../../../core/network/api_client.dart';
import '../models/order_model.dart';

class OrderService {
  final ApiClient _client;

  OrderService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<OrderModel> placeOrder({
    required String addressId,
    required String paymentMethod,
  }) async {
    final data = await _client.post('/orders', body: {
      'addressId': int.parse(addressId),
      'paymentMethod': paymentMethod,
    });
    return OrderModel.fromApi(data as Map<String, dynamic>);
  }

  Future<List<OrderModel>> getOrdersByUser() async {
    final data = await _client.get('/orders');
    return (data as List)
        .map((j) => OrderModel.fromApi(j as Map<String, dynamic>))
        .toList();
  }

  Future<OrderModel?> getOrderById(String orderId) async {
    final data = await _client.get('/orders/$orderId');
    return OrderModel.fromApi(data as Map<String, dynamic>);
  }

  // No WebSocket/SSE endpoint exists for live order-tracking yet —
  // this polls the real GET /orders/{id} endpoint every 20 seconds
  // instead, which is the same "did anything change" information a
  // Firestore snapshot-listener gave, just pulled instead of pushed.
  Stream<OrderModel?> streamOrderById(String orderId) async* {
    while (true) {
      yield await getOrderById(orderId);
      await Future.delayed(const Duration(seconds: 20));
    }
  }

  Future<OrderModel> cancelOrder(String orderId) async {
    final data = await _client.put('/orders/$orderId/cancel');
    return OrderModel.fromApi(data as Map<String, dynamic>);
  }
}