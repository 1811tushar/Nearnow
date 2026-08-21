import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:provider/provider.dart';

import '../providers/product_provider.dart';
import 'product_detail_page.dart';
import '../../../core/constants/app_colors.dart';
import '../../../l10n/app_localizations.dart';

/// Full-screen camera scanner. Points the camera at a barcode, looks the
/// code up against the `barcode` field on products in Firestore, and:
///  - if exactly one product matches, opens that product's detail page
///  - if nothing matches, shows a message and lets the person try again
class BarcodeScannerPage extends StatefulWidget {
  const BarcodeScannerPage({super.key});

  @override
  State<BarcodeScannerPage> createState() => _BarcodeScannerPageState();
}

class _BarcodeScannerPageState extends State<BarcodeScannerPage> {
  final MobileScannerController _controller = MobileScannerController();

  // True while we're already handling a scanned code — this stops the
  // camera from firing the lookup multiple times for the same barcode.
  bool _isProcessing = false;
  String? _statusMessage;

  // null = still checking; true/false = known permission state. Checked
  // up front so a denied camera doesn't just show a blank/broken preview
  // with no explanation.
  bool? _cameraGranted;

  @override
  void initState() {
    super.initState();
    _checkCameraPermission();
  }

  Future<void> _checkCameraPermission() async {
    final status = await Permission.camera.request();
    if (!mounted) return;
    setState(() {
      _cameraGranted = status.isGranted;
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _onDetect(BarcodeCapture capture) async {
    if (_isProcessing) return;

    final barcodes = capture.barcodes;
    final code = barcodes.isNotEmpty ? barcodes.first.rawValue : null;
    if (code == null || code.isEmpty) return;

    final l10n = AppLocalizations.of(context)!;
    setState(() {
      _isProcessing = true;
      _statusMessage = l10n.lookingUpProduct;
    });

    await _controller.stop();
    if (!mounted) return;

    try {
      final product =
          await context.read<ProductProvider>().fetchProductByBarcode(code);
      if (!mounted) return;

      if (product != null) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => ProductDetailPage(productId: product.id),
          ),
        );
      } else {
        await _showMessageAndResume(
          l10n.noProductForBarcode,
        );
      }
    } catch (e) {
      if (!mounted) return;
      await _showMessageAndResume(l10n.somethingWentWrongTryAgain);
    }
  }

  Future<void> _showMessageAndResume(String message) async {
    setState(() => _statusMessage = message);
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;

    setState(() {
      _isProcessing = false;
      _statusMessage = null;
    });
    await _controller.start();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    if (_cameraGranted == false) {
      return Scaffold(
        backgroundColor: Colors.black,
        appBar: AppBar(
          backgroundColor: Colors.black,
          foregroundColor: Colors.white,
          title: Text(l10n.scanBarcode),
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.videocam_off, size: 56, color: Colors.white54),
                const SizedBox(height: 16),
                Text(
                  l10n.cameraPermissionDenied,
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Colors.white),
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: openAppSettings,
                  child: Text(l10n.openSettings),
                ),
              ],
            ),
          ),
        ),
      );
    }

    if (_cameraGranted == null) {
      return const Scaffold(
        backgroundColor: Colors.black,
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        title: Text(l10n.scanBarcode),
        actions: [
          IconButton(
            icon: ValueListenableBuilder(
              valueListenable: _controller,
              builder: (context, state, child) {
                return Icon(
                  state.torchState == TorchState.on
                      ? Icons.flash_on
                      : Icons.flash_off,
                );
              },
            ),
            onPressed: () => _controller.toggleTorch(),
          ),
        ],
      ),
      body: Stack(
        alignment: Alignment.center,
        children: [
          MobileScanner(
            controller: _controller,
            onDetect: _onDetect,
          ),

          // Simple viewfinder frame so the person knows where to aim.
          Container(
            width: 260,
            height: 160,
            decoration: BoxDecoration(
              border: Border.all(color: AppColors.primary, width: 3),
              borderRadius: BorderRadius.circular(12),
            ),
          ),

          Positioned(
            top: 24,
            left: 24,
            right: 24,
            child: Text(
              l10n.pointCameraAtBarcode,
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.white.withValues(alpha: 0.9)),
            ),
          ),

          if (_statusMessage != null)
            Positioned(
              bottom: 60,
              left: 24,
              right: 24,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  vertical: 12,
                  horizontal: 16,
                ),
                decoration: BoxDecoration(
                  color: Colors.black87,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (_isProcessing)
                      const Padding(
                        padding: EdgeInsets.only(right: 12),
                        child: SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    Expanded(
                      child: Text(
                        _statusMessage!,
                        style: const TextStyle(color: Colors.white),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}
