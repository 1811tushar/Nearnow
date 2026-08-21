
import 'package:flutter/material.dart';


class RatingStars extends StatelessWidget {
  final double rating;
  final double size;
  final ValueChanged<double>? onRatingChanged;

  const RatingStars({
    super.key,
    required this.rating,
    this.size = 18,
    this.onRatingChanged,
  });

  @override
  Widget build(BuildContext context) {
    final bool interactive = onRatingChanged != null;

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(5, (index) {
        final starValue = index + 1;
        final filled = rating >= starValue;
        final halfFilled = !filled && rating > index && rating < starValue;

        final icon = filled
            ? Icons.star
            : halfFilled
                ? Icons.star_half
                : Icons.star_border;

        final star = Icon(icon, size: size, color: Colors.amber);

        if (!interactive) return star;

        return GestureDetector(
          onTap: () => onRatingChanged!(starValue.toDouble()),
          child: star,
        );
      }),
    );
  }
}