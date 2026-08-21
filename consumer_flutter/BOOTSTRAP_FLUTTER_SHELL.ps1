$ErrorActionPreference = 'Stop'
Write-Host 'NearNow Flutter shell bootstrap' -ForegroundColor Cyan
if (-not (Get-Command flutter -ErrorAction SilentlyContinue)) {
  throw 'Flutter SDK was not found on PATH. Install Flutter and run: flutter doctor'
}
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$backup = Join-Path $root '.wired_backup'
New-Item -ItemType Directory -Force $backup | Out-Null
Copy-Item -Recurse -Force lib (Join-Path $backup 'lib')
Copy-Item -Force pubspec.yaml (Join-Path $backup 'pubspec.yaml')
Write-Host '1/4 Creating official Flutter platform shell...' -ForegroundColor Green
flutter create --platforms=android,ios,web,windows .
Write-Host '2/4 Restoring the REST-wired NearNow lib/ and pubspec.yaml...' -ForegroundColor Green
Remove-Item -Recurse -Force lib
Copy-Item -Recurse -Force (Join-Path $backup 'lib') lib
Copy-Item -Force (Join-Path $backup 'pubspec.yaml') pubspec.yaml
Write-Host '3/4 Fetching dependencies...' -ForegroundColor Green
flutter pub get
Write-Host '3.5/4 Adding runtime permissions required by barcode/speech/image features...' -ForegroundColor Green
$manifest = Join-Path $root 'android/app/src/main/AndroidManifest.xml'
if (Test-Path $manifest) {
  $xml = Get-Content $manifest -Raw
  if ($xml -notmatch 'android.permission.CAMERA') { $xml = $xml -replace '<manifest([^>]*)>', '<manifest$1>`n    <uses-permission android:name="android.permission.INTERNET" />`n    <uses-permission android:name="android.permission.CAMERA" />`n    <uses-permission android:name="android.permission.RECORD_AUDIO" />' }
  Set-Content -Path $manifest -Value $xml -Encoding UTF8
}
$plist = Join-Path $root 'ios/Runner/Info.plist'
if (Test-Path $plist) {
  $xml = Get-Content $plist -Raw
  if ($xml -notmatch 'NSCameraUsageDescription') { $xml = $xml -replace '</dict>', '  <key>NSCameraUsageDescription</key><string>NearNow uses the camera to scan product barcodes.</string>`n  <key>NSMicrophoneUsageDescription</key><string>NearNow uses the microphone for voice search.</string>`n  <key>NSPhotoLibraryUsageDescription</key><string>NearNow uses your photo library for product images.</string>`n</dict>' }
  Set-Content -Path $plist -Value $xml -Encoding UTF8
}
Write-Host '4/4 Running static analysis...' -ForegroundColor Green
flutter analyze
Write-Host ''
Write-Host 'Flutter shell created and NearNow REST source merged.' -ForegroundColor Cyan
Write-Host 'Run: flutter run' -ForegroundColor Yellow
