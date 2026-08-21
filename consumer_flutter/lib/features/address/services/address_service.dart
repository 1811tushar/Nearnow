import '../../../core/network/api_client.dart';
import '../models/address_model.dart';

class AddressService {
  final ApiClient _client;

  AddressService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<List<AddressModel>> getAddresses() async {
    final data = await _client.get('/addresses');
    return (data as List)
        .map((json) => AddressModel.fromApi(json as Map<String, dynamic>))
        .toList();
  }

  Future<void> addAddress(AddressModel address) async {
    await _client.post('/addresses', body: address.toApi());
  }

  Future<void> updateAddress(AddressModel address) async {
    await _client.put('/addresses/${address.id}', body: address.toApi());
  }

  Future<void> deleteAddress(String addressId) async {
    await _client.delete('/addresses/$addressId');
  }

  Future<void> setDefaultAddress(String addressId) async {
    await _client.put('/addresses/$addressId/set-default');
  }
}