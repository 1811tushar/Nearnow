import '../../../core/network/api_client.dart';
import '../models/cart_item_model.dart';

class CartResult {
  final List<CartItemModel> items;
  final double subtotal;
  final double deliveryFee;
  final double grandTotal;

  CartResult({required this.items, required this.subtotal, required this.deliveryFee, required this.grandTotal});

  factory CartResult.fromApi(Map<String, dynamic> json) {
    final itemsJson = json['items'] as List;
    return CartResult(
      items: itemsJson
          .map((j) => CartItemModel.fromApi(j as Map<String, dynamic>))
          .toList(),
      subtotal: (json['subtotal'] as num?)?.toDouble() ?? 0,
      deliveryFee: (json['deliveryFee'] as num?)?.toDouble() ?? 0,
      grandTotal: (json['grandTotal'] as num?)?.toDouble() ?? 0,
    );
  }
}

class CartService {
  final ApiClient _client;

  CartService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<CartResult> getCart() async {
    final data = await _client.get('/cart');
    return CartResult.fromApi(data as Map<String, dynamic>);
  }

  Future<CartResult> addToCart(CartItemModel item) async {
    final data = await _client.post('/cart/add', body: {
      'productId': int.parse(item.productId),
      'quantity': item.quantity,
    });
    return CartResult.fromApi(data as Map<String, dynamic>);
  }

  Future<CartResult> addMultipleToCart(List<CartItemModel> items) async {
    CartResult? result;
    for (final item in items) {
      result = await addToCart(item);
    }
    return result ?? CartResult(items: [], subtotal: 0, deliveryFee: 0, grandTotal: 0);
  }

  Future<CartResult> removeFromCart(String cartItemId) async {
    final data = await _client.delete('/cart/remove/$cartItemId');
    return CartResult.fromApi(data as Map<String, dynamic>);
  }

  Future<CartResult> updateQuantity(String cartItemId, int newQty) async {
    final data = await _client
        .put('/cart/update-qty/$cartItemId', body: {'quantity': newQty});
    return CartResult.fromApi(data as Map<String, dynamic>);
  }

  Future<void> clearCart() async {
    await _client.delete('/cart/clear');
  }
}