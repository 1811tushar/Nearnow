import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_es.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('es'),
  ];

  /// No description provided for @login.
  ///
  /// In en, this message translates to:
  /// **'Login'**
  String get login;

  /// No description provided for @email.
  ///
  /// In en, this message translates to:
  /// **'Email'**
  String get email;

  /// No description provided for @password.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get password;

  /// No description provided for @loginFailed.
  ///
  /// In en, this message translates to:
  /// **'Login failed'**
  String get loginFailed;

  /// No description provided for @authErrorUserNotFound.
  ///
  /// In en, this message translates to:
  /// **'No account found with this email.'**
  String get authErrorUserNotFound;

  /// No description provided for @authErrorWrongPassword.
  ///
  /// In en, this message translates to:
  /// **'Incorrect password. Please try again.'**
  String get authErrorWrongPassword;

  /// No description provided for @authErrorInvalidCredential.
  ///
  /// In en, this message translates to:
  /// **'Incorrect email or password.'**
  String get authErrorInvalidCredential;

  /// No description provided for @authErrorEmailInUse.
  ///
  /// In en, this message translates to:
  /// **'An account with this email already exists.'**
  String get authErrorEmailInUse;

  /// No description provided for @authErrorWeakPassword.
  ///
  /// In en, this message translates to:
  /// **'Password is too weak. Use at least 6 characters.'**
  String get authErrorWeakPassword;

  /// No description provided for @authErrorInvalidEmail.
  ///
  /// In en, this message translates to:
  /// **'Please enter a valid email address.'**
  String get authErrorInvalidEmail;

  /// No description provided for @authErrorNetworkError.
  ///
  /// In en, this message translates to:
  /// **'Network error. Please check your connection.'**
  String get authErrorNetworkError;

  /// No description provided for @authErrorTooManyRequests.
  ///
  /// In en, this message translates to:
  /// **'Too many attempts. Please try again later.'**
  String get authErrorTooManyRequests;

  /// No description provided for @forgotPassword.
  ///
  /// In en, this message translates to:
  /// **'Forgot Password?'**
  String get forgotPassword;

  /// No description provided for @resetPassword.
  ///
  /// In en, this message translates to:
  /// **'Reset Password'**
  String get resetPassword;

  /// No description provided for @resetPasswordInstructions.
  ///
  /// In en, this message translates to:
  /// **'Enter your email and we\'ll send you a 6-digit code to reset your password.'**
  String get resetPasswordInstructions;

  /// No description provided for @sendResetLink.
  ///
  /// In en, this message translates to:
  /// **'Send Code'**
  String get sendResetLink;

  /// No description provided for @passwordResetEmailSent.
  ///
  /// In en, this message translates to:
  /// **'If that email is registered, a reset code has been sent.'**
  String get passwordResetEmailSent;

  /// No description provided for @resetCodeLabel.
  ///
  /// In en, this message translates to:
  /// **'6-digit code'**
  String get resetCodeLabel;

  /// No description provided for @newPasswordLabel.
  ///
  /// In en, this message translates to:
  /// **'New password'**
  String get newPasswordLabel;

  /// No description provided for @resetPasswordButton.
  ///
  /// In en, this message translates to:
  /// **'Reset password'**
  String get resetPasswordButton;

  /// No description provided for @passwordResetSuccess.
  ///
  /// In en, this message translates to:
  /// **'Password reset. Please log in with your new password.'**
  String get passwordResetSuccess;

  /// No description provided for @useDifferentEmail.
  ///
  /// In en, this message translates to:
  /// **'Use a different email'**
  String get useDifferentEmail;

  /// No description provided for @register.
  ///
  /// In en, this message translates to:
  /// **'Register'**
  String get register;

  /// No description provided for @registrationFailed.
  ///
  /// In en, this message translates to:
  /// **'Registration failed'**
  String get registrationFailed;

  /// No description provided for @groceriesInMinutes.
  ///
  /// In en, this message translates to:
  /// **'Groceries in minutes'**
  String get groceriesInMinutes;

  /// No description provided for @home.
  ///
  /// In en, this message translates to:
  /// **'Home'**
  String get home;

  /// No description provided for @cart.
  ///
  /// In en, this message translates to:
  /// **'Cart'**
  String get cart;

  /// No description provided for @orders.
  ///
  /// In en, this message translates to:
  /// **'Orders'**
  String get orders;

  /// No description provided for @profile.
  ///
  /// In en, this message translates to:
  /// **'Profile'**
  String get profile;

  /// No description provided for @promoNewArrivalsTitle.
  ///
  /// In en, this message translates to:
  /// **'New arrivals'**
  String get promoNewArrivalsTitle;

  /// No description provided for @promoNewArrivalsSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Up to 30% off'**
  String get promoNewArrivalsSubtitle;

  /// No description provided for @promoFreshPicksTitle.
  ///
  /// In en, this message translates to:
  /// **'Fresh picks'**
  String get promoFreshPicksTitle;

  /// No description provided for @promoFreshPicksSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Fruits & vegetables'**
  String get promoFreshPicksSubtitle;

  /// No description provided for @promoQuickEssentialsTitle.
  ///
  /// In en, this message translates to:
  /// **'Quick essentials'**
  String get promoQuickEssentialsTitle;

  /// No description provided for @promoQuickEssentialsSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Free delivery over ₹499'**
  String get promoQuickEssentialsSubtitle;

  /// No description provided for @categories.
  ///
  /// In en, this message translates to:
  /// **'Categories'**
  String get categories;

  /// No description provided for @deliverTo.
  ///
  /// In en, this message translates to:
  /// **'Deliver to: {label}'**
  String deliverTo(String label);

  /// No description provided for @selectDeliveryAddress.
  ///
  /// In en, this message translates to:
  /// **'Select delivery address'**
  String get selectDeliveryAddress;

  /// No description provided for @language.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get language;

  /// No description provided for @searchProductsHint.
  ///
  /// In en, this message translates to:
  /// **'Search products'**
  String get searchProductsHint;

  /// No description provided for @searchCategoryHint.
  ///
  /// In en, this message translates to:
  /// **'Search {category}'**
  String searchCategoryHint(String category);

  /// No description provided for @bestsellers.
  ///
  /// In en, this message translates to:
  /// **'Bestsellers'**
  String get bestsellers;

  /// No description provided for @featuredProducts.
  ///
  /// In en, this message translates to:
  /// **'Featured Products'**
  String get featuredProducts;

  /// No description provided for @shopByCategory.
  ///
  /// In en, this message translates to:
  /// **'Shop by Category'**
  String get shopByCategory;

  /// No description provided for @selectLanguage.
  ///
  /// In en, this message translates to:
  /// **'Select Language'**
  String get selectLanguage;

  /// No description provided for @filters.
  ///
  /// In en, this message translates to:
  /// **'Filters'**
  String get filters;

  /// No description provided for @priceRange.
  ///
  /// In en, this message translates to:
  /// **'Price Range'**
  String get priceRange;

  /// No description provided for @applyFilters.
  ///
  /// In en, this message translates to:
  /// **'Apply Filters'**
  String get applyFilters;

  /// No description provided for @clearAllFilters.
  ///
  /// In en, this message translates to:
  /// **'Clear All Filters'**
  String get clearAllFilters;

  /// No description provided for @products.
  ///
  /// In en, this message translates to:
  /// **'Products'**
  String get products;

  /// No description provided for @allProducts.
  ///
  /// In en, this message translates to:
  /// **'All Products'**
  String get allProducts;

  /// No description provided for @somethingWentWrong.
  ///
  /// In en, this message translates to:
  /// **'Something went wrong'**
  String get somethingWentWrong;

  /// No description provided for @retry.
  ///
  /// In en, this message translates to:
  /// **'Retry'**
  String get retry;

  /// No description provided for @noProductsFound.
  ///
  /// In en, this message translates to:
  /// **'No products found'**
  String get noProductsFound;

  /// No description provided for @tryAdjustingSearch.
  ///
  /// In en, this message translates to:
  /// **'Try adjusting your search or filters'**
  String get tryAdjustingSearch;

  /// No description provided for @speechNotAvailable.
  ///
  /// In en, this message translates to:
  /// **'Speech recognition is not available on this device'**
  String get speechNotAvailable;

  /// No description provided for @productNotFound.
  ///
  /// In en, this message translates to:
  /// **'Product not found'**
  String get productNotFound;

  /// No description provided for @productDetails.
  ///
  /// In en, this message translates to:
  /// **'Product Details'**
  String get productDetails;

  /// No description provided for @ratingReviewsCount.
  ///
  /// In en, this message translates to:
  /// **'{rating} ({count, plural, one{{count} review} other{{count} reviews}})'**
  String ratingReviewsCount(String rating, int count);

  /// No description provided for @inStockCount.
  ///
  /// In en, this message translates to:
  /// **'In Stock ({count})'**
  String inStockCount(int count);

  /// No description provided for @outOfStock.
  ///
  /// In en, this message translates to:
  /// **'Out of Stock'**
  String get outOfStock;

  /// No description provided for @description.
  ///
  /// In en, this message translates to:
  /// **'Description'**
  String get description;

  /// No description provided for @reviews.
  ///
  /// In en, this message translates to:
  /// **'Reviews'**
  String get reviews;

  /// No description provided for @writeAReview.
  ///
  /// In en, this message translates to:
  /// **'Write a Review'**
  String get writeAReview;

  /// No description provided for @noReviewsYet.
  ///
  /// In en, this message translates to:
  /// **'No reviews yet. Be the first to review!'**
  String get noReviewsYet;

  /// No description provided for @anonymous.
  ///
  /// In en, this message translates to:
  /// **'Anonymous'**
  String get anonymous;

  /// No description provided for @addToCart.
  ///
  /// In en, this message translates to:
  /// **'Add to Cart'**
  String get addToCart;

  /// No description provided for @defaultUserName.
  ///
  /// In en, this message translates to:
  /// **'User'**
  String get defaultUserName;

  /// No description provided for @failedToSubmitReview.
  ///
  /// In en, this message translates to:
  /// **'Failed to submit review'**
  String get failedToSubmitReview;

  /// No description provided for @shareYourThoughts.
  ///
  /// In en, this message translates to:
  /// **'Share your thoughts about this product...'**
  String get shareYourThoughts;

  /// No description provided for @submitReview.
  ///
  /// In en, this message translates to:
  /// **'Submit Review'**
  String get submitReview;

  /// No description provided for @addedToCart.
  ///
  /// In en, this message translates to:
  /// **'Added {name} to cart'**
  String addedToCart(String name);

  /// No description provided for @addedQuantityToCart.
  ///
  /// In en, this message translates to:
  /// **'Added {quantity} × {name} to cart'**
  String addedQuantityToCart(int quantity, String name);

  /// No description provided for @addShort.
  ///
  /// In en, this message translates to:
  /// **'ADD'**
  String get addShort;

  /// No description provided for @lookingUpProduct.
  ///
  /// In en, this message translates to:
  /// **'Looking up product...'**
  String get lookingUpProduct;

  /// No description provided for @noProductForBarcode.
  ///
  /// In en, this message translates to:
  /// **'No product found for this barcode. Try another one.'**
  String get noProductForBarcode;

  /// No description provided for @somethingWentWrongTryAgain.
  ///
  /// In en, this message translates to:
  /// **'Something went wrong. Try again.'**
  String get somethingWentWrongTryAgain;

  /// No description provided for @scanBarcode.
  ///
  /// In en, this message translates to:
  /// **'Scan Barcode'**
  String get scanBarcode;

  /// No description provided for @pointCameraAtBarcode.
  ///
  /// In en, this message translates to:
  /// **'Point the camera at a product barcode'**
  String get pointCameraAtBarcode;

  /// No description provided for @takeAPhoto.
  ///
  /// In en, this message translates to:
  /// **'Take a photo'**
  String get takeAPhoto;

  /// No description provided for @chooseFromGallery.
  ///
  /// In en, this message translates to:
  /// **'Choose from gallery'**
  String get chooseFromGallery;

  /// No description provided for @noReadableTextFound.
  ///
  /// In en, this message translates to:
  /// **'No readable text found in that image. Try again.'**
  String get noReadableTextFound;

  /// No description provided for @couldNotReadImage.
  ///
  /// In en, this message translates to:
  /// **'Could not read that image. Try again.'**
  String get couldNotReadImage;

  /// No description provided for @imageSearch.
  ///
  /// In en, this message translates to:
  /// **'Image Search'**
  String get imageSearch;

  /// No description provided for @readingTextFromImage.
  ///
  /// In en, this message translates to:
  /// **'Reading text from image...'**
  String get readingTextFromImage;

  /// No description provided for @noProductsHereYet.
  ///
  /// In en, this message translates to:
  /// **'No products here yet'**
  String get noProductsHereYet;

  /// No description provided for @checkBackSoon.
  ///
  /// In en, this message translates to:
  /// **'Check back soon'**
  String get checkBackSoon;

  /// No description provided for @cashOnDelivery.
  ///
  /// In en, this message translates to:
  /// **'Cash on Delivery'**
  String get cashOnDelivery;

  /// No description provided for @upi.
  ///
  /// In en, this message translates to:
  /// **'UPI'**
  String get upi;

  /// No description provided for @card.
  ///
  /// In en, this message translates to:
  /// **'Card'**
  String get card;

  /// No description provided for @payWhenOrderArrives.
  ///
  /// In en, this message translates to:
  /// **'Pay when your order arrives'**
  String get payWhenOrderArrives;

  /// No description provided for @onlinePaymentsNotEnabled.
  ///
  /// In en, this message translates to:
  /// **'Selection saved; online payments are not enabled yet'**
  String get onlinePaymentsNotEnabled;

  /// No description provided for @paymentMethod.
  ///
  /// In en, this message translates to:
  /// **'Payment method'**
  String get paymentMethod;

  /// No description provided for @myCart.
  ///
  /// In en, this message translates to:
  /// **'My Cart'**
  String get myCart;

  /// No description provided for @cartEmpty.
  ///
  /// In en, this message translates to:
  /// **'Your cart is empty'**
  String get cartEmpty;

  /// No description provided for @addItemsToGetStarted.
  ///
  /// In en, this message translates to:
  /// **'Add items to get started'**
  String get addItemsToGetStarted;

  /// No description provided for @startShopping.
  ///
  /// In en, this message translates to:
  /// **'Start Shopping'**
  String get startShopping;

  /// No description provided for @noDeliveryAddressSelected.
  ///
  /// In en, this message translates to:
  /// **'No delivery address selected'**
  String get noDeliveryAddressSelected;

  /// No description provided for @change.
  ///
  /// In en, this message translates to:
  /// **'Change'**
  String get change;

  /// No description provided for @subtotal.
  ///
  /// In en, this message translates to:
  /// **'Subtotal'**
  String get subtotal;

  /// No description provided for @deliveryFee.
  ///
  /// In en, this message translates to:
  /// **'Delivery Fee'**
  String get deliveryFee;

  /// No description provided for @free.
  ///
  /// In en, this message translates to:
  /// **'FREE'**
  String get free;

  /// No description provided for @total.
  ///
  /// In en, this message translates to:
  /// **'Total'**
  String get total;

  /// No description provided for @placeOrderWithTotal.
  ///
  /// In en, this message translates to:
  /// **'Place Order • ₹{total}'**
  String placeOrderWithTotal(String total);

  /// No description provided for @orderPlacedSuccessfully.
  ///
  /// In en, this message translates to:
  /// **'Order placed successfully'**
  String get orderPlacedSuccessfully;

  /// No description provided for @failedToPlaceOrder.
  ///
  /// In en, this message translates to:
  /// **'Failed to place order'**
  String get failedToPlaceOrder;

  /// No description provided for @noOrdersYet.
  ///
  /// In en, this message translates to:
  /// **'No orders yet'**
  String get noOrdersYet;

  /// No description provided for @orderNumber.
  ///
  /// In en, this message translates to:
  /// **'Order #{id}'**
  String orderNumber(String id);

  /// No description provided for @statusPlaced.
  ///
  /// In en, this message translates to:
  /// **'PLACED'**
  String get statusPlaced;

  /// No description provided for @statusPacked.
  ///
  /// In en, this message translates to:
  /// **'PACKED'**
  String get statusPacked;

  /// No description provided for @statusOutForDelivery.
  ///
  /// In en, this message translates to:
  /// **'OUT FOR DELIVERY'**
  String get statusOutForDelivery;

  /// No description provided for @statusDelivered.
  ///
  /// In en, this message translates to:
  /// **'DELIVERED'**
  String get statusDelivered;

  /// No description provided for @statusCancelled.
  ///
  /// In en, this message translates to:
  /// **'CANCELLED'**
  String get statusCancelled;

  /// No description provided for @itemsAddedToCart.
  ///
  /// In en, this message translates to:
  /// **'Items added to cart'**
  String get itemsAddedToCart;

  /// No description provided for @orderNotFound.
  ///
  /// In en, this message translates to:
  /// **'Order not found'**
  String get orderNotFound;

  /// No description provided for @placedOn.
  ///
  /// In en, this message translates to:
  /// **'Placed on {date}'**
  String placedOn(String date);

  /// No description provided for @deliveringToFull.
  ///
  /// In en, this message translates to:
  /// **'Delivering to: {name}, {address}, {city} - {pincode}'**
  String deliveringToFull(
    String name,
    String address,
    String city,
    String pincode,
  );

  /// No description provided for @items.
  ///
  /// In en, this message translates to:
  /// **'Items'**
  String get items;

  /// No description provided for @reorder.
  ///
  /// In en, this message translates to:
  /// **'Reorder'**
  String get reorder;

  /// No description provided for @cameraPermissionDenied.
  ///
  /// In en, this message translates to:
  /// **'Camera access is needed to scan barcodes. Please enable it in your device settings.'**
  String get cameraPermissionDenied;

  /// No description provided for @openSettings.
  ///
  /// In en, this message translates to:
  /// **'Open Settings'**
  String get openSettings;

  /// No description provided for @cancelOrder.
  ///
  /// In en, this message translates to:
  /// **'Cancel Order'**
  String get cancelOrder;

  /// No description provided for @cancelOrderConfirmTitle.
  ///
  /// In en, this message translates to:
  /// **'Cancel this order?'**
  String get cancelOrderConfirmTitle;

  /// No description provided for @cancelOrderConfirmBody.
  ///
  /// In en, this message translates to:
  /// **'This can\'t be undone. Your order will be cancelled.'**
  String get cancelOrderConfirmBody;

  /// No description provided for @orderCancelled.
  ///
  /// In en, this message translates to:
  /// **'Order cancelled'**
  String get orderCancelled;

  /// No description provided for @failedToCancelOrder.
  ///
  /// In en, this message translates to:
  /// **'Failed to cancel order'**
  String get failedToCancelOrder;

  /// No description provided for @keepOrder.
  ///
  /// In en, this message translates to:
  /// **'Keep Order'**
  String get keepOrder;

  /// No description provided for @yesCancel.
  ///
  /// In en, this message translates to:
  /// **'Yes, Cancel'**
  String get yesCancel;

  /// No description provided for @deleteAddressConfirmTitle.
  ///
  /// In en, this message translates to:
  /// **'Delete this address?'**
  String get deleteAddressConfirmTitle;

  /// No description provided for @deleteAddressConfirmBody.
  ///
  /// In en, this message translates to:
  /// **'This can\'t be undone.'**
  String get deleteAddressConfirmBody;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @invalidPincode.
  ///
  /// In en, this message translates to:
  /// **'Enter a valid 6-digit pincode'**
  String get invalidPincode;

  /// No description provided for @myAddresses.
  ///
  /// In en, this message translates to:
  /// **'My Addresses'**
  String get myAddresses;

  /// No description provided for @pleaseLogInToViewAddresses.
  ///
  /// In en, this message translates to:
  /// **'Please log in to view addresses.'**
  String get pleaseLogInToViewAddresses;

  /// No description provided for @pleaseLogInToContinue.
  ///
  /// In en, this message translates to:
  /// **'Please log in to continue'**
  String get pleaseLogInToContinue;

  /// No description provided for @noSavedAddressesYet.
  ///
  /// In en, this message translates to:
  /// **'No saved addresses yet'**
  String get noSavedAddressesYet;

  /// No description provided for @tapAddressToUse.
  ///
  /// In en, this message translates to:
  /// **'Tap an address to use it for delivery'**
  String get tapAddressToUse;

  /// No description provided for @nowDeliveringTo.
  ///
  /// In en, this message translates to:
  /// **'Now delivering to: {label}'**
  String nowDeliveringTo(String label);

  /// No description provided for @selected.
  ///
  /// In en, this message translates to:
  /// **'SELECTED'**
  String get selected;

  /// No description provided for @addressSavedNoLocation.
  ///
  /// In en, this message translates to:
  /// **'Address saved, but we couldn\'t pinpoint its exact location'**
  String get addressSavedNoLocation;

  /// No description provided for @editAddress.
  ///
  /// In en, this message translates to:
  /// **'Edit Address'**
  String get editAddress;

  /// No description provided for @addAddress.
  ///
  /// In en, this message translates to:
  /// **'Add Address'**
  String get addAddress;

  /// No description provided for @label.
  ///
  /// In en, this message translates to:
  /// **'Label'**
  String get label;

  /// No description provided for @fullName.
  ///
  /// In en, this message translates to:
  /// **'Full name'**
  String get fullName;

  /// No description provided for @address.
  ///
  /// In en, this message translates to:
  /// **'Address'**
  String get address;

  /// No description provided for @city.
  ///
  /// In en, this message translates to:
  /// **'City'**
  String get city;

  /// No description provided for @pincode.
  ///
  /// In en, this message translates to:
  /// **'Pincode'**
  String get pincode;

  /// No description provided for @locatingAddress.
  ///
  /// In en, this message translates to:
  /// **'Locating address...'**
  String get locatingAddress;

  /// No description provided for @saveAddress.
  ///
  /// In en, this message translates to:
  /// **'Save Address'**
  String get saveAddress;

  /// No description provided for @labelHomeWork.
  ///
  /// In en, this message translates to:
  /// **'Label (Home/Work) *'**
  String get labelHomeWork;

  /// No description provided for @fullNameRequired.
  ///
  /// In en, this message translates to:
  /// **'Full Name *'**
  String get fullNameRequired;

  /// No description provided for @phoneRequired.
  ///
  /// In en, this message translates to:
  /// **'Phone *'**
  String get phoneRequired;

  /// No description provided for @addressLineRequired.
  ///
  /// In en, this message translates to:
  /// **'Address Line *'**
  String get addressLineRequired;

  /// No description provided for @cityRequired.
  ///
  /// In en, this message translates to:
  /// **'City *'**
  String get cityRequired;

  /// No description provided for @pincodeRequired.
  ///
  /// In en, this message translates to:
  /// **'Pincode *'**
  String get pincodeRequired;

  /// No description provided for @myWishlist.
  ///
  /// In en, this message translates to:
  /// **'My Wishlist'**
  String get myWishlist;

  /// No description provided for @wishlistEmpty.
  ///
  /// In en, this message translates to:
  /// **'Your wishlist is empty'**
  String get wishlistEmpty;

  /// No description provided for @myProfile.
  ///
  /// In en, this message translates to:
  /// **'My Profile'**
  String get myProfile;

  /// No description provided for @accountSettings.
  ///
  /// In en, this message translates to:
  /// **'Account Settings'**
  String get accountSettings;

  /// No description provided for @wishlist.
  ///
  /// In en, this message translates to:
  /// **'Wishlist'**
  String get wishlist;

  /// No description provided for @savedAddresses.
  ///
  /// In en, this message translates to:
  /// **'Saved Addresses'**
  String get savedAddresses;

  /// No description provided for @pushNotifications.
  ///
  /// In en, this message translates to:
  /// **'Push Notifications'**
  String get pushNotifications;

  /// No description provided for @receiveOrderUpdates.
  ///
  /// In en, this message translates to:
  /// **'Receive order status updates'**
  String get receiveOrderUpdates;

  /// No description provided for @generateBarcodes.
  ///
  /// In en, this message translates to:
  /// **'Generate Barcodes for Products'**
  String get generateBarcodes;

  /// No description provided for @seedDatabase.
  ///
  /// In en, this message translates to:
  /// **'Seed Database (Debug Only)'**
  String get seedDatabase;

  /// No description provided for @seedingDatabase.
  ///
  /// In en, this message translates to:
  /// **'Seeding database...'**
  String get seedingDatabase;

  /// No description provided for @databaseSeeded.
  ///
  /// In en, this message translates to:
  /// **'Database seeded successfully'**
  String get databaseSeeded;

  /// No description provided for @databaseAlreadySeeded.
  ///
  /// In en, this message translates to:
  /// **'Database was already seeded — nothing to do'**
  String get databaseAlreadySeeded;

  /// No description provided for @oneTimeSetupSafe.
  ///
  /// In en, this message translates to:
  /// **'One-time setup — safe to tap again'**
  String get oneTimeSetupSafe;

  /// No description provided for @generatingBarcodes.
  ///
  /// In en, this message translates to:
  /// **'Generating barcodes...'**
  String get generatingBarcodes;

  /// No description provided for @addedBarcodesTo.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, one{Added barcode to {count} product} other{Added barcodes to {count} products}}'**
  String addedBarcodesTo(int count);

  /// No description provided for @allProductsHaveBarcodes.
  ///
  /// In en, this message translates to:
  /// **'All products already have barcodes.'**
  String get allProductsHaveBarcodes;

  /// No description provided for @logout.
  ///
  /// In en, this message translates to:
  /// **'Logout'**
  String get logout;

  /// No description provided for @profileUpdatedSuccessfully.
  ///
  /// In en, this message translates to:
  /// **'Profile updated successfully'**
  String get profileUpdatedSuccessfully;

  /// No description provided for @failedToUpdateProfile.
  ///
  /// In en, this message translates to:
  /// **'Failed to update profile'**
  String get failedToUpdateProfile;

  /// No description provided for @editProfile.
  ///
  /// In en, this message translates to:
  /// **'Edit Profile'**
  String get editProfile;

  /// No description provided for @edit.
  ///
  /// In en, this message translates to:
  /// **'Edit'**
  String get edit;

  /// No description provided for @delete.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get delete;

  /// No description provided for @viewAll.
  ///
  /// In en, this message translates to:
  /// **'View All'**
  String get viewAll;

  /// No description provided for @pleaseEnterYourName.
  ///
  /// In en, this message translates to:
  /// **'Please enter your name'**
  String get pleaseEnterYourName;

  /// No description provided for @phoneNumber.
  ///
  /// In en, this message translates to:
  /// **'Phone Number'**
  String get phoneNumber;

  /// No description provided for @saveChanges.
  ///
  /// In en, this message translates to:
  /// **'Save Changes'**
  String get saveChanges;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'es'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'es':
      return AppLocalizationsEs();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
