import 'package:flutter/material.dart';
import '../models/address_model.dart';
import '../services/address_service.dart';

class AddressProvider extends ChangeNotifier {
  final AddressService _addressService;

  AddressProvider({AddressService? addressService})
      : _addressService = addressService ?? AddressService();

  List<AddressModel> _addresses = [];
  bool _isLoading = false;
  String? _error;

  List<AddressModel> get addresses => _addresses;
  bool get isLoading => _isLoading;
  String? get error => _error;

  AddressModel? get selectedAddress {
    if (_addresses.isEmpty) return null;
    return _addresses.firstWhere(
      (a) => a.isDefault,
      orElse: () => _addresses.first,
    );
  }

  Future<void> fetchAddresses(String uid) async {
    try {
      _isLoading = true;
      notifyListeners();

      _addresses = await _addressService.getAddresses();
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> addAddress(String uid, AddressModel address) async {
    try {
      _isLoading = true;
      notifyListeners();

      // The very first address a user saves should automatically become
      // the selected delivery address — otherwise nothing is ever marked
      // default and the app has no address to show at checkout.
      final addressToSave =
          _addresses.isEmpty ? address.copyWith(isDefault: true) : address;

      await _addressService.addAddress(addressToSave);
      await fetchAddresses(uid);
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    } finally {
      _isLoading = false;
    }
  }

  Future<void> updateAddress(String uid, AddressModel address) async {
    try {
      await _addressService.updateAddress(address);
      await fetchAddresses(uid);
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  /// Deletes an address. If the deleted address was the default one, the
  /// next remaining address (if any) is automatically promoted to default
  /// so the app never ends up with saved addresses but no default —
  /// previously this was silently masked by a `.first` fallback in the UI
  /// while the underlying data stayed inconsistent.
  Future<void> deleteAddress(String uid, String addressId) async {
    try {
      final wasDefault = _addresses
          .firstWhere((a) => a.id == addressId,
              orElse: () => _addresses.first)
          .isDefault;

      await _addressService.deleteAddress(addressId);
      _addresses.removeWhere((a) => a.id == addressId);

      if (wasDefault && _addresses.isNotEmpty) {
        await _addressService.setDefaultAddress(_addresses.first.id);
        await fetchAddresses(uid);
        return;
      }

      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  Future<void> setDefaultAddress(String uid, String addressId) async {
    try {
      await _addressService.setDefaultAddress(addressId);
      await fetchAddresses(uid);
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  void clear() {
    _addresses = [];
    _error = null;
    _isLoading = false;
    notifyListeners();
  }
}
