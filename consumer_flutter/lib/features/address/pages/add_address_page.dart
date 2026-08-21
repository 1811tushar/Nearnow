import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/address_model.dart';
import '../providers/address_provider.dart';
import '../../auth/providers/auth_provider.dart';
import '../../../core/widgets/primary_button.dart';
import '../../../core/utils/validators.dart';
import '../../../core/utils/geocoding_util.dart';
import '../../../l10n/app_localizations.dart';

class AddAddressPage extends StatefulWidget {
  final AddressModel? existing;

  const AddAddressPage({super.key, this.existing});

  @override
  State<AddAddressPage> createState() => _AddAddressPageState();
}

class _AddAddressPageState extends State<AddAddressPage> {
  final _formKey = GlobalKey<FormState>();

  late final TextEditingController _labelController;
  late final TextEditingController _nameController;
  late final TextEditingController _phoneController;
  late final TextEditingController _addressLineController;
  late final TextEditingController _cityController;
  late final TextEditingController _pincodeController;

  bool _isLocating = false;

  @override
  void initState() {
    super.initState();
    final e = widget.existing;
    _labelController = TextEditingController(text: e?.label ?? '');
    _nameController = TextEditingController(text: e?.fullName ?? '');
    _phoneController = TextEditingController(text: e?.phone ?? '');
    _addressLineController = TextEditingController(text: e?.addressLine ?? '');
    _cityController = TextEditingController(text: e?.city ?? '');
    _pincodeController = TextEditingController(text: e?.pincode ?? '');
  }

  @override
  void dispose() {
    _labelController.dispose();
    _nameController.dispose();
    _phoneController.dispose();
    _addressLineController.dispose();
    _cityController.dispose();
    _pincodeController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;

    final uid = context.read<AuthProvider>().user?.uid;
    if (uid == null) return;

    final l10n = AppLocalizations.of(context)!;
    setState(() => _isLocating = true);

   final query =
        "${_cityController.text.trim()} ${_pincodeController.text.trim()}";
    final geocoded = await GeocodingUtil.geocode(query);

    if (!mounted) return;
    setState(() => _isLocating = false);

    final addressProvider = context.read<AddressProvider>();

    final address = AddressModel(
      id: widget.existing?.id ?? '',
      label: _labelController.text.trim(),
      fullName: _nameController.text.trim(),
      phone: _phoneController.text.trim(),
      addressLine: _addressLineController.text.trim(),
      city: _cityController.text.trim(),
      pincode: _pincodeController.text.trim(),
      latitude: geocoded?.lat ?? widget.existing?.latitude ?? 0.0,
      longitude: geocoded?.lng ?? widget.existing?.longitude ?? 0.0,
      isDefault: widget.existing?.isDefault ?? false,
    );

    if (widget.existing != null) {
      await addressProvider.updateAddress(uid, address);
    } else {
      await addressProvider.addAddress(uid, address);
    }

    if (!mounted) return;

    if (geocoded == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
              l10n.addressSavedNoLocation),
        ),
      );
    }

    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    final addressProvider = context.watch<AddressProvider>();
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.existing != null ? l10n.editAddress : l10n.addAddress),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              TextFormField(
                controller: _labelController,
                decoration: InputDecoration(labelText: l10n.labelHomeWork),
                validator: (v) => Validators.requiredField(v, l10n.label),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _nameController,
                decoration: InputDecoration(labelText: l10n.fullNameRequired),
                validator: (v) => Validators.requiredField(v, l10n.fullName),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                decoration: InputDecoration(labelText: l10n.phoneRequired),
                validator: Validators.phone,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _addressLineController,
                decoration: InputDecoration(labelText: l10n.addressLineRequired),
                validator: (v) => Validators.requiredField(v, l10n.address),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _cityController,
                decoration: InputDecoration(labelText: l10n.cityRequired),
                validator: (v) => Validators.requiredField(v, l10n.city),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _pincodeController,
                keyboardType: TextInputType.number,
                decoration: InputDecoration(labelText: l10n.pincodeRequired),
                validator: Validators.pincode,
              ),
              const SizedBox(height: 32),
              PrimaryButton(
                label: _isLocating ? l10n.locatingAddress : l10n.saveAddress,
                isLoading: addressProvider.isLoading || _isLocating,
                onPressed: _isLocating ? null : _save,
              ),
            ],
          ),
        ),
      ),
    );
  }
}