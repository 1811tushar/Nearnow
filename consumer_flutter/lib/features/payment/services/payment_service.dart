import '../../../core/network/api_client.dart';

class PaymentOrder {
  final String paymentReference;
  final double amount;
  final String currency;
  final String mode;
  // Only populated when mode == 'RAZORPAY'. This is Razorpay's PUBLIC
  // key id — safe to hold client-side, needed to open the Razorpay
  // checkout sheet.
  final String? razorpayKeyId;

  const PaymentOrder({
    required this.paymentReference,
    required this.amount,
    required this.currency,
    required this.mode,
    this.razorpayKeyId,
  });

  factory PaymentOrder.fromApi(Map<String, dynamic> json) => PaymentOrder(
    paymentReference: json['paymentReference'] as String,
    amount: (json['amount'] as num).toDouble(),
    currency: json['currency'] as String,
    mode: json['mode'] as String,
    razorpayKeyId: json['razorpayKeyId'] as String?,
  );
}

class PaymentService {
  final ApiClient _client;
  PaymentService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<PaymentOrder> createOrder() async {
    final data = await _client.post('/payments/create-order');
    return PaymentOrder.fromApi(data as Map<String, dynamic>);
  }

  /// MOCK-mode verification: a simulated SUCCESS/FAILED outcome chosen
  /// in-app, no real gateway involved.
  Future<dynamic> verifyMockPayment({
    required String paymentReference,
    required String outcome,
    required String addressId,
  }) {
    return _client.post('/payments/verify', body: {
      'paymentReference': paymentReference,
      'outcome': outcome,
      'addressId': int.parse(addressId),
    });
  }

  /// Real Razorpay verification: these three fields are exactly what
  /// Razorpay's own checkout success callback hands back — the backend
  /// re-derives the expected signature from paymentId+orderId and its
  /// own secret key, and only places the order if they match.
  Future<dynamic> verifyRazorpayPayment({
    required String razorpayOrderId,
    required String razorpayPaymentId,
    required String razorpaySignature,
    required String addressId,
  }) {
    return _client.post('/payments/verify', body: {
      'paymentReference': razorpayOrderId,
      'razorpayOrderId': razorpayOrderId,
      'razorpayPaymentId': razorpayPaymentId,
      'razorpaySignature': razorpaySignature,
      'addressId': int.parse(addressId),
    });
  }
}
