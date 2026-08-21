// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Spanish Castilian (`es`).
class AppLocalizationsEs extends AppLocalizations {
  AppLocalizationsEs([String locale = 'es']) : super(locale);

  @override
  String get login => 'Iniciar sesión';

  @override
  String get email => 'Correo electrónico';

  @override
  String get password => 'Contraseña';

  @override
  String get loginFailed => 'Error al iniciar sesión';

  @override
  String get authErrorUserNotFound =>
      'No se encontró ninguna cuenta con este correo.';

  @override
  String get authErrorWrongPassword =>
      'Contraseña incorrecta. Inténtalo de nuevo.';

  @override
  String get authErrorInvalidCredential => 'Correo o contraseña incorrectos.';

  @override
  String get authErrorEmailInUse => 'Ya existe una cuenta con este correo.';

  @override
  String get authErrorWeakPassword =>
      'La contraseña es demasiado débil. Usa al menos 6 caracteres.';

  @override
  String get authErrorInvalidEmail => 'Ingresa un correo electrónico válido.';

  @override
  String get authErrorNetworkError => 'Error de red. Verifica tu conexión.';

  @override
  String get authErrorTooManyRequests =>
      'Demasiados intentos. Inténtalo más tarde.';

  @override
  String get forgotPassword => '¿Olvidaste tu contraseña?';

  @override
  String get resetPassword => 'Restablecer contraseña';

  @override
  String get resetPasswordInstructions =>
      'Ingresa tu correo y te enviaremos un código de 6 dígitos para restablecer tu contraseña.';

  @override
  String get sendResetLink => 'Enviar código';

  @override
  String get passwordResetEmailSent =>
      'Si ese correo está registrado, se ha enviado un código.';

  @override
  String get resetCodeLabel => 'Código de 6 dígitos';

  @override
  String get newPasswordLabel => 'Nueva contraseña';

  @override
  String get resetPasswordButton => 'Restablecer contraseña';

  @override
  String get passwordResetSuccess =>
      'Contraseña restablecida. Inicia sesión con tu nueva contraseña.';

  @override
  String get useDifferentEmail => 'Usar otro correo';

  @override
  String get register => 'Registrarse';

  @override
  String get registrationFailed => 'Error en el registro';

  @override
  String get groceriesInMinutes => 'Comestibles en minutos';

  @override
  String get home => 'Inicio';

  @override
  String get cart => 'Carrito';

  @override
  String get orders => 'Pedidos';

  @override
  String get profile => 'Perfil';

  @override
  String get promoNewArrivalsTitle => 'Novedades';

  @override
  String get promoNewArrivalsSubtitle => 'Hasta 30% de descuento';

  @override
  String get promoFreshPicksTitle => 'Selección fresca';

  @override
  String get promoFreshPicksSubtitle => 'Frutas y verduras';

  @override
  String get promoQuickEssentialsTitle => 'Esenciales rápidos';

  @override
  String get promoQuickEssentialsSubtitle =>
      'Envío gratis en pedidos superiores a ₹499';

  @override
  String get categories => 'Categorías';

  @override
  String deliverTo(String label) {
    return 'Entregar a: $label';
  }

  @override
  String get selectDeliveryAddress => 'Seleccionar dirección de entrega';

  @override
  String get language => 'Idioma';

  @override
  String get searchProductsHint => 'Buscar productos';

  @override
  String searchCategoryHint(String category) {
    return 'Buscar $category';
  }

  @override
  String get bestsellers => 'Más vendidos';

  @override
  String get featuredProducts => 'Productos destacados';

  @override
  String get shopByCategory => 'Comprar por categoría';

  @override
  String get selectLanguage => 'Seleccionar idioma';

  @override
  String get filters => 'Filtros';

  @override
  String get priceRange => 'Rango de precios';

  @override
  String get applyFilters => 'Aplicar filtros';

  @override
  String get clearAllFilters => 'Borrar todos los filtros';

  @override
  String get products => 'Productos';

  @override
  String get allProducts => 'Todos los productos';

  @override
  String get somethingWentWrong => 'Algo salió mal';

  @override
  String get retry => 'Reintentar';

  @override
  String get noProductsFound => 'No se encontraron productos';

  @override
  String get tryAdjustingSearch => 'Intenta ajustar tu búsqueda o filtros';

  @override
  String get speechNotAvailable =>
      'El reconocimiento de voz no está disponible en este dispositivo';

  @override
  String get productNotFound => 'Producto no encontrado';

  @override
  String get productDetails => 'Detalles del producto';

  @override
  String ratingReviewsCount(String rating, int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count reseñas',
      one: '$count reseña',
    );
    return '$rating ($_temp0)';
  }

  @override
  String inStockCount(int count) {
    return 'En stock ($count)';
  }

  @override
  String get outOfStock => 'Agotado';

  @override
  String get description => 'Descripción';

  @override
  String get reviews => 'Reseñas';

  @override
  String get writeAReview => 'Escribir una reseña';

  @override
  String get noReviewsYet => 'Aún no hay reseñas. ¡Sé el primero en opinar!';

  @override
  String get anonymous => 'Anónimo';

  @override
  String get addToCart => 'Añadir al carrito';

  @override
  String get defaultUserName => 'Usuario';

  @override
  String get failedToSubmitReview => 'No se pudo enviar la reseña';

  @override
  String get shareYourThoughts => 'Comparte tu opinión sobre este producto...';

  @override
  String get submitReview => 'Enviar reseña';

  @override
  String addedToCart(String name) {
    return '$name añadido al carrito';
  }

  @override
  String addedQuantityToCart(int quantity, String name) {
    return '$quantity × $name añadido al carrito';
  }

  @override
  String get addShort => 'AÑADIR';

  @override
  String get lookingUpProduct => 'Buscando producto...';

  @override
  String get noProductForBarcode =>
      'No se encontró ningún producto para este código de barras. Prueba con otro.';

  @override
  String get somethingWentWrongTryAgain =>
      'Algo salió mal. Inténtalo de nuevo.';

  @override
  String get scanBarcode => 'Escanear código de barras';

  @override
  String get pointCameraAtBarcode =>
      'Apunta la cámara al código de barras del producto';

  @override
  String get takeAPhoto => 'Tomar una foto';

  @override
  String get chooseFromGallery => 'Elegir de la galería';

  @override
  String get noReadableTextFound =>
      'No se encontró texto legible en esa imagen. Inténtalo de nuevo.';

  @override
  String get couldNotReadImage =>
      'No se pudo leer esa imagen. Inténtalo de nuevo.';

  @override
  String get imageSearch => 'Búsqueda por imagen';

  @override
  String get readingTextFromImage => 'Leyendo texto de la imagen...';

  @override
  String get noProductsHereYet => 'Aún no hay productos aquí';

  @override
  String get checkBackSoon => 'Vuelve a revisar pronto';

  @override
  String get cashOnDelivery => 'Pago contra entrega';

  @override
  String get upi => 'UPI';

  @override
  String get card => 'Tarjeta';

  @override
  String get payWhenOrderArrives => 'Paga cuando llegue tu pedido';

  @override
  String get onlinePaymentsNotEnabled =>
      'Selección guardada; los pagos en línea aún no están habilitados';

  @override
  String get paymentMethod => 'Método de pago';

  @override
  String get myCart => 'Mi carrito';

  @override
  String get cartEmpty => 'Tu carrito está vacío';

  @override
  String get addItemsToGetStarted => 'Añade artículos para empezar';

  @override
  String get startShopping => 'Empezar a comprar';

  @override
  String get noDeliveryAddressSelected =>
      'No se seleccionó dirección de entrega';

  @override
  String get change => 'Cambiar';

  @override
  String get subtotal => 'Subtotal';

  @override
  String get deliveryFee => 'Costo de envío';

  @override
  String get free => 'GRATIS';

  @override
  String get total => 'Total';

  @override
  String placeOrderWithTotal(String total) {
    return 'Realizar pedido • ₹$total';
  }

  @override
  String get orderPlacedSuccessfully => 'Pedido realizado con éxito';

  @override
  String get failedToPlaceOrder => 'No se pudo realizar el pedido';

  @override
  String get noOrdersYet => 'Aún no hay pedidos';

  @override
  String orderNumber(String id) {
    return 'Pedido #$id';
  }

  @override
  String get statusPlaced => 'REALIZADO';

  @override
  String get statusPacked => 'EMPACADO';

  @override
  String get statusOutForDelivery => 'EN CAMINO';

  @override
  String get statusDelivered => 'ENTREGADO';

  @override
  String get statusCancelled => 'CANCELADO';

  @override
  String get itemsAddedToCart => 'Artículos añadidos al carrito';

  @override
  String get orderNotFound => 'Pedido no encontrado';

  @override
  String placedOn(String date) {
    return 'Realizado el $date';
  }

  @override
  String deliveringToFull(
    String name,
    String address,
    String city,
    String pincode,
  ) {
    return 'Entregando a: $name, $address, $city - $pincode';
  }

  @override
  String get items => 'Artículos';

  @override
  String get reorder => 'Volver a pedir';

  @override
  String get cameraPermissionDenied =>
      'Se necesita acceso a la cámara para escanear códigos de barras. Actívalo en la configuración de tu dispositivo.';

  @override
  String get openSettings => 'Abrir configuración';

  @override
  String get cancelOrder => 'Cancelar pedido';

  @override
  String get cancelOrderConfirmTitle => '¿Cancelar este pedido?';

  @override
  String get cancelOrderConfirmBody =>
      'Esto no se puede deshacer. Tu pedido será cancelado.';

  @override
  String get orderCancelled => 'Pedido cancelado';

  @override
  String get failedToCancelOrder => 'No se pudo cancelar el pedido';

  @override
  String get keepOrder => 'Mantener pedido';

  @override
  String get yesCancel => 'Sí, cancelar';

  @override
  String get deleteAddressConfirmTitle => '¿Eliminar esta dirección?';

  @override
  String get deleteAddressConfirmBody => 'Esto no se puede deshacer.';

  @override
  String get cancel => 'Cancelar';

  @override
  String get invalidPincode => 'Ingresa un código postal válido de 6 dígitos';

  @override
  String get myAddresses => 'Mis direcciones';

  @override
  String get pleaseLogInToViewAddresses =>
      'Inicia sesión para ver las direcciones.';

  @override
  String get pleaseLogInToContinue => 'Inicia sesión para continuar';

  @override
  String get noSavedAddressesYet => 'Aún no hay direcciones guardadas';

  @override
  String get tapAddressToUse => 'Toca una dirección para usarla en la entrega';

  @override
  String nowDeliveringTo(String label) {
    return 'Entregando ahora a: $label';
  }

  @override
  String get selected => 'SELECCIONADO';

  @override
  String get addressSavedNoLocation =>
      'Dirección guardada, pero no pudimos ubicar su localización exacta';

  @override
  String get editAddress => 'Editar dirección';

  @override
  String get addAddress => 'Añadir dirección';

  @override
  String get label => 'Etiqueta';

  @override
  String get fullName => 'Nombre completo';

  @override
  String get address => 'Dirección';

  @override
  String get city => 'Ciudad';

  @override
  String get pincode => 'Código postal';

  @override
  String get locatingAddress => 'Localizando dirección...';

  @override
  String get saveAddress => 'Guardar dirección';

  @override
  String get labelHomeWork => 'Etiqueta (Casa/Trabajo) *';

  @override
  String get fullNameRequired => 'Nombre completo *';

  @override
  String get phoneRequired => 'Teléfono *';

  @override
  String get addressLineRequired => 'Dirección *';

  @override
  String get cityRequired => 'Ciudad *';

  @override
  String get pincodeRequired => 'Código postal *';

  @override
  String get myWishlist => 'Mi lista de deseos';

  @override
  String get wishlistEmpty => 'Tu lista de deseos está vacía';

  @override
  String get myProfile => 'Mi perfil';

  @override
  String get accountSettings => 'Configuración de la cuenta';

  @override
  String get wishlist => 'Lista de deseos';

  @override
  String get savedAddresses => 'Direcciones guardadas';

  @override
  String get pushNotifications => 'Notificaciones push';

  @override
  String get receiveOrderUpdates =>
      'Recibir actualizaciones del estado del pedido';

  @override
  String get generateBarcodes => 'Generar códigos de barras para productos';

  @override
  String get seedDatabase => 'Sembrar base de datos (solo depuración)';

  @override
  String get seedingDatabase => 'Sembrando base de datos...';

  @override
  String get databaseSeeded => 'Base de datos sembrada con éxito';

  @override
  String get databaseAlreadySeeded =>
      'La base de datos ya estaba sembrada — nada que hacer';

  @override
  String get oneTimeSetupSafe => 'Configuración única: seguro tocar de nuevo';

  @override
  String get generatingBarcodes => 'Generando códigos de barras...';

  @override
  String addedBarcodesTo(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Se añadieron códigos de barras a $count productos',
      one: 'Se añadió código de barras a $count producto',
    );
    return '$_temp0';
  }

  @override
  String get allProductsHaveBarcodes =>
      'Todos los productos ya tienen códigos de barras.';

  @override
  String get logout => 'Cerrar sesión';

  @override
  String get profileUpdatedSuccessfully => 'Perfil actualizado con éxito';

  @override
  String get failedToUpdateProfile => 'No se pudo actualizar el perfil';

  @override
  String get editProfile => 'Editar perfil';

  @override
  String get edit => 'Editar';

  @override
  String get delete => 'Eliminar';

  @override
  String get viewAll => 'Ver todo';

  @override
  String get pleaseEnterYourName => 'Por favor ingresa tu nombre';

  @override
  String get phoneNumber => 'Número de teléfono';

  @override
  String get saveChanges => 'Guardar cambios';
}
