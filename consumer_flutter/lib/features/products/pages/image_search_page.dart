import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';

import 'product_list_page.dart';
import '../../../l10n/app_localizations.dart';

/// Lets the person take a photo (or pick one from the gallery) of a product
/// label, runs FREE on-device text recognition on it (Google ML Kit — no
/// paid API, no network call), and searches the catalog using whatever
/// text was found on the label.
class ImageSearchPage extends StatefulWidget {
  const ImageSearchPage({super.key});

  @override
  State<ImageSearchPage> createState() => _ImageSearchPageState();
}

class _ImageSearchPageState extends State<ImageSearchPage> {
  bool _isProcessing = false;

  @override
  void initState() {
    super.initState();
    // Ask "camera or gallery" as soon as this page opens.
    WidgetsBinding.instance.addPostFrameCallback((_) => _showSourcePicker());
  }

  Future<void> _showSourcePicker() async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (context) {
        final l10n = AppLocalizations.of(context)!;
        return SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.photo_camera_outlined),
              title: Text(l10n.takeAPhoto),
              onTap: () => Navigator.pop(context, ImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: Text(l10n.chooseFromGallery),
              onTap: () => Navigator.pop(context, ImageSource.gallery),
            ),
          ],
        ),
        );
      },
    );

    if (source == null) {
      // Person dismissed the sheet without choosing — go back.
      if (mounted) Navigator.of(context).pop();
      return;
    }

    await _pickAndProcess(source);
  }

  Future<void> _pickAndProcess(ImageSource source) async {
    setState(() => _isProcessing = true);
    final l10n = AppLocalizations.of(context)!;

    try {
      final picked = await ImagePicker().pickImage(source: source);
      if (picked == null) {
        if (mounted) Navigator.of(context).pop();
        return;
      }

      final recognizer = TextRecognizer(script: TextRecognitionScript.latin);
      final inputImage = InputImage.fromFilePath(picked.path);
      final result = await recognizer.processImage(inputImage);
      await recognizer.close();

      final extractedText = _bestSearchTerm(result);

      if (!mounted) return;

      if (extractedText.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.noReadableTextFound),
          ),
        );
        Navigator.of(context).pop();
        return;
      }

      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => ProductListPage(initialSearchQuery: extractedText, semanticSearch: true),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.couldNotReadImage)),
      );
      Navigator.of(context).pop();
    }
  }

  /// ML Kit gives us every line of text it saw on the label (brand, weight,
  /// ingredients, etc), each with its own bounding box. The product name is
  /// almost always printed in the largest font on the label — so we pick
  /// the line with the tallest bounding box (a reliable proxy for font
  /// size) rather than just the longest line of characters, which used to
  /// get fooled by long ingredient lists or barcodes-as-text printed in a
  /// tiny font.
  String _bestSearchTerm(RecognizedText recognizedText) {
    TextLine? tallestLine;
    double tallestHeight = 0;

    for (final block in recognizedText.blocks) {
      for (final line in block.lines) {
        final text = line.text.trim();
        if (text.isEmpty) continue;
        final height = line.boundingBox.height;
        if (height > tallestHeight) {
          tallestHeight = height;
          tallestLine = line;
        }
      }
    }

    if (tallestLine != null) return tallestLine.text.trim();

    // Fallback for the rare case ML Kit returns text with no usable
    // bounding boxes — same longest-line heuristic as before.
    final lines = recognizedText.text
        .split('\n')
        .map((line) => line.trim())
        .where((line) => line.isNotEmpty)
        .toList();
    if (lines.isEmpty) return '';
    lines.sort((a, b) => b.length.compareTo(a.length));
    return lines.first;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(title: Text(l10n.imageSearch)),
      body: Center(
        child: _isProcessing
            ? Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const CircularProgressIndicator(),
                  const SizedBox(height: 16),
                  Text(l10n.readingTextFromImage),
                ],
              )
            : const SizedBox.shrink(),
      ),
    );
  }
}
