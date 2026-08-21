
import 'package:flutter/material.dart';
import '../constants/app_spacing.dart';
import '../../l10n/app_localizations.dart';

class SectionHeader extends StatelessWidget {
  final String title;
  final VoidCallback? onViewAll;

  const SectionHeader({super.key, required this.title, this.onViewAll});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Padding(
      padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(title, style: Theme.of(context).textTheme.headlineSmall),
          if (onViewAll != null)
            TextButton(onPressed: onViewAll, child: Text(l10n.viewAll)),
        ],
      ),
    );
  }
}