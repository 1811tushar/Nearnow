
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';

import 'features/auth/providers/auth_provider.dart';
import 'features/auth/pages/login_page.dart';
import 'features/profile/providers/user_provider.dart';

import 'features/products/providers/product_provider.dart';
import 'features/cart/providers/cart_provider.dart';
import 'features/wishlist/providers/wishlist_provider.dart';
import 'features/orders/providers/order_provider.dart';
import 'features/address/providers/address_provider.dart';
import 'features/category/providers/category_provider.dart';
import 'features/reviews/providers/review_provider.dart';
import 'core/theme/app_theme.dart';
import 'core/constants/app_constants.dart';
import 'core/pages/splash_page.dart';
import 'core/pages/app_shell.dart';
import 'core/providers/locale_provider.dart';
import 'l10n/app_localizations.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final localeProvider = LocaleProvider();
  await localeProvider.loadSavedLocale();

  final userProvider = UserProvider();
  final authProvider = AuthProvider();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: userProvider),
        ChangeNotifierProvider.value(value: authProvider),
        ChangeNotifierProvider(create: (_) => ProductProvider()),
        ChangeNotifierProvider(create: (_) => CartProvider()),
        ChangeNotifierProvider(create: (_) => OrderProvider()),
        ChangeNotifierProvider(create: (_) => WishlistProvider()),
        ChangeNotifierProvider(create: (_) => AddressProvider()),
        ChangeNotifierProvider(create: (_) => ReviewProvider()),
        ChangeNotifierProvider(create: (_) => CategoryProvider()),
        ChangeNotifierProvider.value(value: localeProvider),
      ],
      // tryAutoLogin runs AFTER runApp — SplashPage stays visible while
      // it checks for a saved token, exactly the "restore session before
      // showing Login/Home" moment Firebase used to handle invisibly.
      child: _AppStartup(authProvider: authProvider, userProvider: userProvider),
    ),
  );
}

class _AppStartup extends StatefulWidget {
  final AuthProvider authProvider;
  final UserProvider userProvider;

  const _AppStartup({required this.authProvider, required this.userProvider});

  @override
  State<_AppStartup> createState() => _AppStartupState();
}

class _AppStartupState extends State<_AppStartup> {
  @override
  void initState() {
    super.initState();
    widget.authProvider.tryAutoLogin(widget.userProvider);
  }

  @override
  Widget build(BuildContext context) => const MyApp();
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    final localeProvider = context.watch<LocaleProvider>();

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: AppConstants.appName,
      theme: AppTheme.lightTheme,
      locale: localeProvider.locale,
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: SplashPage(nextScreen: const AuthWrapper()),
    );
  }
}

class AuthWrapper extends StatelessWidget {
  const AuthWrapper({super.key});

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();

    if (auth.isInitializing) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (!auth.isLoggedIn) {
      return const LoginPage();
    }
    return const AppShell();
  }
}