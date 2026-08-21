import 'package:flutter/material.dart';

import '../../features/cart/pages/cart_page.dart';
import '../../features/home/pages/home_page.dart';
import '../../features/orders/pages/order_history_page.dart';
import '../../features/profile/pages/view_profile_page.dart';
import '../constants/app_colors.dart';
import '../../l10n/app_localizations.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key});

  @override
  State<AppShell> createState() => AppShellState();

  static AppShellState? of(BuildContext context) {
    return context.findAncestorStateOfType<AppShellState>();
  }
}

class AppShellState extends State<AppShell> {
  int _selectedIndex = 0;

  final List<Widget> _tabs = const [
    HomePage(),
    CartPage(),
    OrderHistoryPage(),
    ViewProfilePage(),
  ];

  void switchToTab(int index) {
    if (index == _selectedIndex) return;

    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      body: IndexedStack(
        index: _selectedIndex,
        children: _tabs,
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: switchToTab,
        indicatorColor: AppColors.primary.withValues(alpha: 0.14),
        destinations: [
          NavigationDestination(
            icon: const Icon(Icons.home_outlined),
            selectedIcon: const Icon(Icons.home),
            label: l10n.home,
          ),
          NavigationDestination(
            icon: const Icon(Icons.shopping_cart_outlined),
            selectedIcon: const Icon(Icons.shopping_cart),
            label: l10n.cart,
          ),
          NavigationDestination(
            icon: const Icon(Icons.receipt_long_outlined),
            selectedIcon: const Icon(Icons.receipt_long),
            label: l10n.orders,
          ),
          NavigationDestination(
            icon: const Icon(Icons.person_outline),
            selectedIcon: const Icon(Icons.person),
            label: l10n.profile,
          ),
        ],
      ),
    );
  }
}