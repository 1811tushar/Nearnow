# NearNow Flutter Consumer

This folder contains the REST-wired NearNow consumer source.

## Official Flutter platform shell

If your existing project already contains `android/`, `ios/`, `web/`, `windows/`, `test/`, `analysis_options.yaml` and the rest of the platform shell, keep those folders and merge/replace the NearNow Dart source and `pubspec.yaml` changes from this bundle.

If you need a shell, use `BOOTSTRAP_FLUTTER_SHELL.ps1`.

## API base URL

Android emulator:

```powershell
flutter run --dart-define=NEARNOW_API_BASE_URL=http://10.0.2.2:8080/api
```

Physical device:

```powershell
flutter run --dart-define=NEARNOW_API_BASE_URL=http://192.168.1.10:8080/api
```

## Security

The JWT signing secret never enters Flutter. Flutter stores only the issued access token using `flutter_secure_storage`.

## Payment

The default/free build uses a **Mock Online Payment** gateway. It does not include `razorpay_flutter`, does not require Razorpay credentials, and cannot charge real money.

A future production adapter can be added behind the payment service abstraction without changing the order/inventory domain model.
