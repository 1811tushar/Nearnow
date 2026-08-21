import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/address_provider.dart';
import '../../auth/providers/auth_provider.dart';
import '../../../core/widgets/loading_widget.dart';
import '../../../core/widgets/empty_state_widget.dart';
import 'add_address_page.dart';
import '../../../l10n/app_localizations.dart';

class AddressListPage extends StatefulWidget {
  final String? uid;

  const AddressListPage({super.key, this.uid});

  @override
  State<AddressListPage> createState() => _AddressListPageState();
}

class _AddressListPageState extends State<AddressListPage> {
  String? _resolveUid(BuildContext context) {
    return widget.uid ?? context.read<AuthProvider>().user?.uid;
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final uid = _resolveUid(context);
      if (uid != null) {
        context.read<AddressProvider>().fetchAddresses(uid);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final addressProvider = context.watch<AddressProvider>();
    final uid = _resolveUid(context);
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.myAddresses)),
      body: uid == null
          ? Center(child: Text(l10n.pleaseLogInToViewAddresses))
          : addressProvider.isLoading
              ? const LoadingWidget()
              : addressProvider.error != null
                  ? EmptyStateWidget(
                      icon: Icons.error_outline,
                      title: l10n.somethingWentWrong,
                      subtitle: addressProvider.error,
                      actionLabel: l10n.retry,
                      onAction: () => addressProvider.fetchAddresses(uid),
                    )
                  : addressProvider.addresses.isEmpty
                  ? EmptyStateWidget(
                      icon: Icons.location_on_outlined,
                      title: l10n.noSavedAddressesYet,
                    )
                  : ListView(
                      padding: const EdgeInsets.all(12),
                      children: [
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
                          child: Text(
                            l10n.tapAddressToUse,
                            style: const TextStyle(color: Colors.grey, fontSize: 13),
                          ),
                        ),
                        ...addressProvider.addresses.map((address) {
                          return Card(
                            color: address.isDefault
                                ? Colors.green.withValues(alpha: 0.06)
                                : null,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8),
                              side: BorderSide(
                                color: address.isDefault
                                    ? Colors.green
                                    : Colors.transparent,
                                width: 1.2,
                              ),
                            ),
                            child: ListTile(
                              onTap: () {
                                if (address.isDefault) return;
                                addressProvider.setDefaultAddress(uid, address.id);
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                    content: Text(
                                      l10n.nowDeliveringTo(address.label),
                                    ),
                                    duration: const Duration(seconds: 2),
                                  ),
                                );
                              },
                              leading: Icon(
                                address.isDefault
                                    ? Icons.radio_button_checked
                                    : Icons.radio_button_off,
                                color: address.isDefault ? Colors.green : null,
                              ),
                              title: Row(
                                children: [
                                  Text(address.label),
                                  if (address.isDefault) ...[
                                    const SizedBox(width: 8),
                                    Container(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 6,
                                        vertical: 1,
                                      ),
                                      decoration: BoxDecoration(
                                        color: Colors.green,
                                        borderRadius: BorderRadius.circular(4),
                                      ),
                                      child: Text(
                                        l10n.selected,
                                        style: const TextStyle(
                                          color: Colors.white,
                                          fontSize: 10,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                              subtitle: Text(
                                "${address.addressLine}, ${address.city} - ${address.pincode}",
                              ),
                              trailing: PopupMenuButton<String>(
                                onSelected: (value) async {
                                  if (value == 'edit') {
                                    Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                        builder: (_) =>
                                            AddAddressPage(existing: address),
                                      ),
                                    );
                                  } else if (value == 'delete') {
                                    final confirmed = await showDialog<bool>(
                                      context: context,
                                      builder: (dialogContext) => AlertDialog(
                                        title: Text(
                                            l10n.deleteAddressConfirmTitle),
                                        content: Text(
                                            l10n.deleteAddressConfirmBody),
                                        actions: [
                                          TextButton(
                                            onPressed: () => Navigator.pop(
                                                dialogContext, false),
                                            child: Text(l10n.cancel),
                                          ),
                                          TextButton(
                                            onPressed: () => Navigator.pop(
                                                dialogContext, true),
                                            child: Text(l10n.delete,
                                                style: const TextStyle(
                                                    color: Colors.red)),
                                          ),
                                        ],
                                      ),
                                    );
                                    if (confirmed == true) {
                                      addressProvider.deleteAddress(
                                          uid, address.id);
                                    }
                                  }
                                },
                                itemBuilder: (context) => [
                                  PopupMenuItem(
                                    value: 'edit',
                                    child: Text(l10n.edit),
                                  ),
                                  PopupMenuItem(
                                    value: 'delete',
                                    child: Text(l10n.delete),
                                  ),
                                ],
                              ),
                            ),
                          );
                        }),
                      ],
                    ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          if (uid == null) return;
          Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const AddAddressPage()),
          );
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
