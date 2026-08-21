import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:provider/provider.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/widgets/loading_widget.dart';
import '../../../core/widgets/empty_state_widget.dart';
import '../../address/pages/address_list_page.dart';
import '../../auth/providers/auth_provider.dart';
import '../../../core/pages/app_shell.dart';
import '../../wishlist/pages/wishlist_page.dart';
import '../../admin/services/seed_service.dart';
import '../providers/user_provider.dart';
import 'edit_profile_page.dart';
import '../../../l10n/app_localizations.dart';
import '../../../core/widgets/language_picker_sheet.dart';

class ViewProfilePage extends StatefulWidget {
  const ViewProfilePage({super.key});

  @override
  State<ViewProfilePage> createState() => _ViewProfilePageState();
}

class _ViewProfilePageState extends State<ViewProfilePage> {
  @override
  Widget build(BuildContext context) {
    final userProvider = context.watch<UserProvider>();
    final user = userProvider.user;
    final l10n = AppLocalizations.of(context)!;

    if (userProvider.isLoading) {
      return const Scaffold(
        body: Center(child: LoadingWidget()),
      );
    }

    if (userProvider.error != null && user == null) {
      return Scaffold(
        appBar: AppBar(title: Text(l10n.myProfile)),
        body: EmptyStateWidget(
          icon: Icons.error_outline,
          title: l10n.somethingWentWrong,
          subtitle: userProvider.error,
          actionLabel: l10n.retry,
          onAction: () {
            userProvider.loadCurrentUser();
          },
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.myProfile),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const EditProfilePage()),
              );
            },
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.md),
        children: [
          // Header Card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.md),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 30,
                    backgroundColor: AppColors.primary.withValues(alpha: 0.1),
                    child: Text(
                      user?.fullName.isNotEmpty == true
                          ? user!.fullName[0].toUpperCase()
                          : (user?.email.isNotEmpty == true
                              ? user!.email[0].toUpperCase()
                              : 'U'),
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user?.fullName.isNotEmpty == true
                              ? user!.fullName
                              : l10n.defaultUserName,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                fontWeight: FontWeight.bold,
                              ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          user?.email ?? '',
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                color: Theme.of(context).textTheme.bodySmall?.color,
                              ),
                        ),
                        if (user?.phone.isNotEmpty == true) ...[
                          const SizedBox(height: 2),
                          Text(
                            user!.phone,
                            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                  color: Theme.of(context).textTheme.bodySmall?.color,
                                ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),

          // Menu Section
          Text(
            l10n.accountSettings,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: Theme.of(context).textTheme.bodyMedium?.color,
            ),
          ),
          const SizedBox(height: AppSpacing.xs),

          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.shopping_bag_outlined, color: AppColors.primary),
                  title: Text(l10n.orders),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {
                    if (user != null) {
                      AppShell.of(context)?.switchToTab(2);
                    }
                  },
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.favorite_border, color: AppColors.primary),
                  title: Text(l10n.wishlist),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => WishlistPage(uid: user?.uid ?? ''),
                      ),
                    );
                  },
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.location_on_outlined, color: AppColors.primary),
                  title: Text(l10n.savedAddresses),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => AddressListPage(uid: user?.uid),
                      ),
                    );
                  },
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.language, color: AppColors.primary),
                  title: Text(l10n.language),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => showLanguagePicker(context),
                ),
                const Divider(height: 1),
                SwitchListTile(
                  secondary: const Icon(Icons.notifications_outlined, color: AppColors.primary),
                  title: Text(l10n.pushNotifications),
                  subtitle: Text(l10n.receiveOrderUpdates),
                  value: user?.notificationsEnabled ?? true,
                  activeColor: AppColors.primary,
                  onChanged: (bool value) async {
                    await userProvider.toggleNotifications(value);
                  },
                ),
                if (user?.role == 'admin') ...[
                  const Divider(height: 1),
                  // Debug-only AND admin-only: a fresh Firebase project
                  // can now be seeded from inside the app instead of
                  // requiring manual Firestore Console work. Gated by
                  // kDebugMode as well as role so it can never appear in
                  // a release build regardless of who's signed in.
                  if (kDebugMode)
                    ListTile(
                      leading: const Icon(Icons.cloud_upload,
                          color: AppColors.primary),
                      title: Text(l10n.seedDatabase),
                      onTap: () async {
                        final messenger = ScaffoldMessenger.of(context);
                        final seedService = SeedService();
                        final alreadySeeded =
                            await seedService.isAlreadySeeded();
                        if (alreadySeeded) {
                          messenger.showSnackBar(
                            SnackBar(
                                content: Text(l10n.databaseAlreadySeeded)),
                          );
                          return;
                        }
                        messenger.showSnackBar(
                          SnackBar(content: Text(l10n.seedingDatabase)),
                        );
                        await seedService.seedAll();
                        messenger.showSnackBar(
                          SnackBar(content: Text(l10n.databaseSeeded)),
                        );
                      },
                    ),
                ],
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.lg),

          // Logout Button
          OutlinedButton.icon(
            style: OutlinedButton.styleFrom(
              foregroundColor: AppColors.error,
              side: const BorderSide(color: AppColors.error),
              padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
            ),
            icon: const Icon(Icons.logout),
            label: Text(l10n.logout),
            onPressed: () async {
              await context.read<AuthProvider>().logout(userProvider);
            },
          ),
        ],
      ),
    );
  }
}