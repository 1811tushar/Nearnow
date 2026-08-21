import 'package:flutter/material.dart';
import '../models/cart_item_model.dart';
import '../services/cart_service.dart';

class CartProvider extends ChangeNotifier {
  final CartService _cartService;

  CartProvider({CartService? cartService})
      : _cartService = cartService ?? CartService();

  List<CartItemModel> _items = [];
  double _subtotal = 0;
  double _deliveryFee = 0;
  double _grandTotal = 0;
  bool _isLoading = false;
  String? _error;

  List<CartItemModel> get items => _items;
  bool get isLoading => _isLoading;
  String? get error => _error;

  int get itemCount => _items.fold(0, (sum, item) => sum + item.quantity);

  double get subtotal => _subtotal;
  double get deliveryFee => _deliveryFee;
  double get grandTotal => _grandTotal;

  Future<void> fetchCart(String uid) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final result = await _cartService.getCart();
      _items = result.items;
      _subtotal = result.subtotal; _deliveryFee = result.deliveryFee; _grandTotal = result.grandTotal;
    } catch (e) {
      _error = e.toString();
    }

    _isLoading = false;
    notifyListeners();
  }

  Future<void> addToCart(String uid, CartItemModel item) async {
    try {
      final result = await _cartService.addToCart(item);
      _items = result.items;
      _subtotal = result.subtotal; _deliveryFee = result.deliveryFee; _grandTotal = result.grandTotal;
      _error = null;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  Future<void> addMultipleToCart(String uid, List<CartItemModel> items) async {
    try {
      final result = await _cartService.addMultipleToCart(items);
      _items = result.items;
      _subtotal = result.subtotal; _deliveryFee = result.deliveryFee; _grandTotal = result.grandTotal;
      _error = null;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  Future<void> removeFromCart(String uid, String cartItemId) async {
    try {
      final result = await _cartService.removeFromCart(cartItemId);
      _items = result.items;
      _subtotal = result.subtotal; _deliveryFee = result.deliveryFee; _grandTotal = result.grandTotal;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  Future<void> updateQuantity(String uid, String cartItemId, int newQty) async {
    if (newQty < 1) return;
    try {
      final result = await _cartService.updateQuantity(cartItemId, newQty);
      _items = result.items;
      _subtotal = result.subtotal; _deliveryFee = result.deliveryFee; _grandTotal = result.grandTotal;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  void clearLocalCart() {
    _items = []; _subtotal = 0; _deliveryFee = 0; _grandTotal = 0;
    notifyListeners();
  }

  Future<void> clearCart(String uid) async {
    try {
      await _cartService.clearCart();
      _items = []; _subtotal = 0; _deliveryFee = 0; _grandTotal = 0;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }
}