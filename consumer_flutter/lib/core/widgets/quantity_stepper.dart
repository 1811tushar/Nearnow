
import 'package:flutter/material.dart';
import '../constants/app_colors.dart';
import '../constants/app_radius.dart';
import '../constants/app_spacing.dart';

/// A reusable +/- quantity stepper used across Product Detail, Cart,
/// and anywhere else grocery-style quantity selection is needed.
///
/// Pass [min] (default 1) and [max] (e.g. product.stock) to bound the range.
/// [onChanged] fires with the new quantity whenever + or - is tapped.
class QuantityStepper extends StatelessWidget {
  final int quantity;
  final int min;
  final int? max;
  final ValueChanged<int> onChanged;
  final double iconSize;

  const QuantityStepper({
    super.key,
    required this.quantity,
    required this.onChanged,
    this.min = 1,
    this.max,
    this.iconSize = 18,
  });

  bool get _canDecrement => quantity > min;
  bool get _canIncrement => max == null || quantity < max!;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.circular(AppRadius.button),
border: Border.all(color: AppColors.grey.withValues(alpha: 0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _StepperButton(
            icon: Icons.remove,
            enabled: _canDecrement,
            iconSize: iconSize,
            onTap: () {
              if (_canDecrement) onChanged(quantity - 1);
            },
          ),
          SizedBox(
            width: 32,
            child: Text(
              '$quantity',
              textAlign: TextAlign.center,
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(fontWeight: FontWeight.bold),
            ),
          ),
          _StepperButton(
            icon: Icons.add,
            enabled: _canIncrement,
            iconSize: iconSize,
            onTap: () {
              if (_canIncrement) onChanged(quantity + 1);
            },
          ),
        ],
      ),
    );
  }
}

class _StepperButton extends StatelessWidget {
  final IconData icon;
  final bool enabled;
  final double iconSize;
  final VoidCallback onTap;

  const _StepperButton({
    required this.icon,
    required this.enabled,
    required this.iconSize,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: enabled ? onTap : null,
      borderRadius: BorderRadius.circular(AppRadius.button),
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.sm),
        child: Icon(
          icon,
          size: iconSize,
          color: enabled ? AppColors.primary : AppColors.grey,
        ),
      ),
    );
  }
}