import '../../../core/network/api_client.dart';

class PaymentOrder {
  final String paymentReference;
  final double amount;
  final String currency;
  final String mode;

  const PaymentOrder({required this.paymentReference, required this.amount, required this.currency, required this.mode});

  factory PaymentOrder.fromApi(Map<String, dynamic> json) => PaymentOrder(
    paymentReference: json['paymentReference'] as String,
    amount: (json['amount'] as num).toDouble(),
    currency: json['currency'] as String,
    mode: json['mode'] as String,
  );
}

class PaymentService {
  final ApiClient _client;
  PaymentService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<PaymentOrder> createOrder() async {
    final data = await _client.post('/payments/create-order');
    return PaymentOrder.fromApi(data as Map<String, dynamic>);
  }

  Future<dynamic> verifyPayment({required String paymentReference, required String outcome, required String addressId}) {
    return _client.post('/payments/verify', body: {
      'paymentReference': paymentReference,
      'outcome': outcome,
      'addressId': int.parse(addressId),
    });
  }
}
