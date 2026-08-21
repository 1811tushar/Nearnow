// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get login => 'Login';

  @override
  String get email => 'Email';

  @override
  String get password => 'Password';

  @override
  String get loginFailed => 'Login failed';

  @override
  String get authErrorUserNotFound => 'No account found with this email.';

  @override
  String get authErrorWrongPassword => 'Incorrect password. Please try again.';

  @override
  String get authErrorInvalidCredential => 'Incorrect email or password.';

  @override
  String get authErrorEmailInUse =>
      'An account with this email already exists.';

  @override
  String get authErrorWeakPassword =>
      'Password is too weak. Use at least 6 characters.';

  @override
  String get authErrorInvalidEmail => 'Please enter a valid email address.';

  @override
  String get authErrorNetworkError =>
      'Network error. Please check your connection.';

  @override
  String get authErrorTooManyRequests =>
      'Too many attempts. Please try again later.';

  @override
  String get forgotPassword => 'Forgot Password?';

  @override
  String get resetPassword => 'Reset Password';

  @override
  String get resetPasswordInstructions =>
      'Enter your email and we\'ll send you a 6-digit code to reset your password.';

  @override
  String get sendResetLink => 'Send Code';

  @override
  String get passwordResetEmailSent =>
      'If that email is registered, a reset code has been sent.';

  @override
  String get resetCodeLabel => '6-digit code';

  @override
  String get newPasswordLabel => 'New password';

  @override
  String get resetPasswordButton => 'Reset password';

  @override
  String get passwordResetSuccess =>
      'Password reset. Please log in with your new password.';

  @override
  String get useDifferentEmail => 'Use a different email';

  @override
  String get register => 'Register';

  @override
  String get registrationFailed => 'Registration failed';

  @override
  String get groceriesInMinutes => 'Groceries in minutes';

  @override
  String get home => 'Home';

  @override
  String get cart => 'Cart';

  @override
  String get orders => 'Orders';

  @override
  String get profile => 'Profile';

  @override
  String get promoNewArrivalsTitle => 'New arrivals';

  @override
  String get promoNewArrivalsSubtitle => 'Up to 30% off';

  @override
  String get promoFreshPicksTitle => 'Fresh picks';

  @override
  String get promoFreshPicksSubtitle => 'Fruits & vegetables';

  @override
  String get promoQuickEssentialsTitle => 'Quick essentials';

  @override
  String get promoQuickEssentialsSubtitle => 'Free delivery over ₹499';

  @override
  String get categories => 'Categories';

  @override
  String deliverTo(String label) {
    return 'Deliver to: $label';
  }

  @override
  String get selectDeliveryAddress => 'Select delivery address';

  @override
  String get language => 'Language';

  @override
  String get searchProductsHint => 'Search products';

  @override
  String searchCategoryHint(String category) {
    return 'Search $category';
  }

  @override
  String get bestsellers => 'Bestsellers';

  @override
  String get featuredProducts => 'Featured Products';

  @override
  String get shopByCategory => 'Shop by Category';

  @override
  String get selectLanguage => 'Select Language';

  @override
  String get filters => 'Filters';

  @override
  String get priceRange => 'Price Range';

  @override
  String get applyFilters => 'Apply Filters';

  @override
  String get clearAllFilters => 'Clear All Filters';

  @override
  String get products => 'Products';

  @override
  String get allProducts => 'All Products';

  @override
  String get somethingWentWrong => 'Something went wrong';

  @override
  String get retry => 'Retry';

  @override
  String get noProductsFound => 'No products found';

  @override
  String get tryAdjustingSearch => 'Try adjusting your search or filters';

  @override
  String get speechNotAvailable =>
      'Speech recognition is not available on this device';

  @override
  String get productNotFound => 'Product not found';

  @override
  String get productDetails => 'Product Details';

  @override
  String ratingReviewsCount(String rating, int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count reviews',
      one: '$count review',
    );
    return '$rating ($_temp0)';
  }

  @override
  String inStockCount(int count) {
    return 'In Stock ($count)';
  }

  @override
  String get outOfStock => 'Out of Stock';

  @override
  String get description => 'Description';

  @override
  String get reviews => 'Reviews';

  @override
  String get writeAReview => 'Write a Review';

  @override
  String get noReviewsYet => 'No reviews yet. Be the first to review!';

  @override
  String get anonymous => 'Anonymous';

  @override
  String get addToCart => 'Add to Cart';

  @override
  String get defaultUserName => 'User';

  @override
  String get failedToSubmitReview => 'Failed to submit review';

  @override
  String get shareYourThoughts => 'Share your thoughts about this product...';

  @override
  String get submitReview => 'Submit Review';

  @override
  String addedToCart(String name) {
    return 'Added $name to cart';
  }

  @override
  String addedQuantityToCart(int quantity, String name) {
    return 'Added $quantity × $name to cart';
  }

  @override
  String get addShort => 'ADD';

  @override
  String get lookingUpProduct => 'Looking up product...';

  @override
  String get noProductForBarcode =>
      'No product found for this barcode. Try another one.';

  @override
  String get somethingWentWrongTryAgain => 'Something went wrong. Try again.';

  @override
  String get scanBarcode => 'Scan Barcode';

  @override
  String get pointCameraAtBarcode => 'Point the camera at a product barcode';

  @override
  String get takeAPhoto => 'Take a photo';

  @override
  String get chooseFromGallery => 'Choose from gallery';

  @override
  String get noReadableTextFound =>
      'No readable text found in that image. Try again.';

  @override
  String get couldNotReadImage => 'Could not read that image. Try again.';

  @override
  String get imageSearch => 'Image Search';

  @override
  String get readingTextFromImage => 'Reading text from image...';

  @override
  String get noProductsHereYet => 'No products here yet';

  @override
  String get checkBackSoon => 'Check back soon';

  @override
  String get cashOnDelivery => 'Cash on Delivery';

  @override
  String get upi => 'UPI';

  @override
  String get card => 'Card';

  @override
  String get payWhenOrderArrives => 'Pay when your order arrives';

  @override
  String get onlinePaymentsNotEnabled =>
      'Selection saved; online payments are not enabled yet';

  @override
  String get paymentMethod => 'Payment method';

  @override
  String get myCart => 'My Cart';

  @override
  String get cartEmpty => 'Your cart is empty';

  @override
  String get addItemsToGetStarted => 'Add items to get started';

  @override
  String get startShopping => 'Start Shopping';

  @override
  String get noDeliveryAddressSelected => 'No delivery address selected';

  @override
  String get change => 'Change';

  @override
  String get subtotal => 'Subtotal';

  @override
  String get deliveryFee => 'Delivery Fee';

  @override
  String get free => 'FREE';

  @override
  String get total => 'Total';

  @override
  String placeOrderWithTotal(String total) {
    return 'Place Order • ₹$total';
  }

  @override
  String get orderPlacedSuccessfully => 'Order placed successfully';

  @override
  String get failedToPlaceOrder => 'Failed to place order';

  @override
  String get noOrdersYet => 'No orders yet';

  @override
  String orderNumber(String id) {
    return 'Order #$id';
  }

  @override
  String get statusPlaced => 'PLACED';

  @override
  String get statusPacked => 'PACKED';

  @override
  String get statusOutForDelivery => 'OUT FOR DELIVERY';

  @override
  String get statusDelivered => 'DELIVERED';

  @override
  String get statusCancelled => 'CANCELLED';

  @override
  String get itemsAddedToCart => 'Items added to cart';

  @override
  String get orderNotFound => 'Order not found';

  @override
  String placedOn(String date) {
    return 'Placed on $date';
  }

  @override
  String deliveringToFull(
    String name,
    String address,
    String city,
    String pincode,
  ) {
    return 'Delivering to: $name, $address, $city - $pincode';
  }

  @override
  String get items => 'Items';

  @override
  String get reorder => 'Reorder';

  @override
  String get cameraPermissionDenied =>
      'Camera access is needed to scan barcodes. Please enable it in your device settings.';

  @override
  String get openSettings => 'Open Settings';

  @override
  String get cancelOrder => 'Cancel Order';

  @override
  String get cancelOrderConfirmTitle => 'Cancel this order?';

  @override
  String get cancelOrderConfirmBody =>
      'This can\'t be undone. Your order will be cancelled.';

  @override
  String get orderCancelled => 'Order cancelled';

  @override
  String get failedToCancelOrder => 'Failed to cancel order';

  @override
  String get keepOrder => 'Keep Order';

  @override
  String get yesCancel => 'Yes, Cancel';

  @override
  String get deleteAddressConfirmTitle => 'Delete this address?';

  @override
  String get deleteAddressConfirmBody => 'This can\'t be undone.';

  @override
  String get cancel => 'Cancel';

  @override
  String get invalidPincode => 'Enter a valid 6-digit pincode';

  @override
  String get myAddresses => 'My Addresses';

  @override
  String get pleaseLogInToViewAddresses => 'Please log in to view addresses.';

  @override
  String get pleaseLogInToContinue => 'Please log in to continue';

  @override
  String get noSavedAddressesYet => 'No saved addresses yet';

  @override
  String get tapAddressToUse => 'Tap an address to use it for delivery';

  @override
  String nowDeliveringTo(String label) {
    return 'Now delivering to: $label';
  }

  @override
  String get selected => 'SELECTED';

  @override
  String get addressSavedNoLocation =>
      'Address saved, but we couldn\'t pinpoint its exact location';

  @override
  String get editAddress => 'Edit Address';

  @override
  String get addAddress => 'Add Address';

  @override
  String get label => 'Label';

  @override
  String get fullName => 'Full name';

  @override
  String get address => 'Address';

  @override
  String get city => 'City';

  @override
  String get pincode => 'Pincode';

  @override
  String get locatingAddress => 'Locating address...';

  @override
  String get saveAddress => 'Save Address';

  @override
  String get labelHomeWork => 'Label (Home/Work) *';

  @override
  String get fullNameRequired => 'Full Name *';

  @override
  String get phoneRequired => 'Phone *';

  @override
  String get addressLineRequired => 'Address Line *';

  @override
  String get cityRequired => 'City *';

  @override
  String get pincodeRequired => 'Pincode *';

  @override
  String get myWishlist => 'My Wishlist';

  @override
  String get wishlistEmpty => 'Your wishlist is empty';

  @override
  String get myProfile => 'My Profile';

  @override
  String get accountSettings => 'Account Settings';

  @override
  String get wishlist => 'Wishlist';

  @override
  String get savedAddresses => 'Saved Addresses';

  @override
  String get pushNotifications => 'Push Notifications';

  @override
  String get receiveOrderUpdates => 'Receive order status updates';

  @override
  String get generateBarcodes => 'Generate Barcodes for Products';

  @override
  String get seedDatabase => 'Seed Database (Debug Only)';

  @override
  String get seedingDatabase => 'Seeding database...';

  @override
  String get databaseSeeded => 'Database seeded successfully';

  @override
  String get databaseAlreadySeeded =>
      'Database was already seeded — nothing to do';

  @override
  String get oneTimeSetupSafe => 'One-time setup — safe to tap again';

  @override
  String get generatingBarcodes => 'Generating barcodes...';

  @override
  String addedBarcodesTo(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Added barcodes to $count products',
      one: 'Added barcode to $count product',
    );
    return '$_temp0';
  }

  @override
  String get allProductsHaveBarcodes => 'All products already have barcodes.';

  @override
  String get logout => 'Logout';

  @override
  String get profileUpdatedSuccessfully => 'Profile updated successfully';

  @override
  String get failedToUpdateProfile => 'Failed to update profile';

  @override
  String get editProfile => 'Edit Profile';

  @override
  String get edit => 'Edit';

  @override
  String get delete => 'Delete';

  @override
  String get viewAll => 'View All';

  @override
  String get pleaseEnterYourName => 'Please enter your name';

  @override
  String get phoneNumber => 'Phone Number';

  @override
  String get saveChanges => 'Save Changes';
}
