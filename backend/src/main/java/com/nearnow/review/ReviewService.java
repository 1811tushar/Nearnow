package com.nearnow.review;

import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.common.exception.InvalidOperationException;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.order.OrderItemRepository;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                          UserRepository userRepository, OrderItemRepository orderItemRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // @Transactional: review save + product rating/reviewCount update
    // must succeed or fail together — the direct Java equivalent of the
    // old Firestore runTransaction() block. Without this, a crash between
    // the two writes could leave a saved review with a stale product
    // rating, same failure-mode the original code's own comment warned about.
    //
    // @CacheEvict: this is the matching half of ProductService.getProductById()'s
    // own comment — without this, a cached product would keep showing
    // its PRE-review rating/reviewCount indefinitely after every review.
    // Caught while building Phase 12, not left as a dangling gap.
    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public ReviewResponseDTO submitReview(String userEmail, Long productId, ReviewRequestDTO request) {
        User user = getUser(userEmail);
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        // NEW check, added deliberately (owner-confirmed) — did not exist
        // in the old Firestore version at all (verified: no purchase-check
        // anywhere in review_service.dart or review_provider.dart). Likely
        // absent there because Orders didn't exist as a dependency when
        // Reviews was originally built. Closes a real fake-review/spam gap.
        boolean hasPurchased = orderItemRepository
                .existsByOrder_User_IdAndProduct_IdAndOrder_Status(user.getId(), productId, com.nearnow.order.OrderStatus.DELIVERED);
        if (!hasPurchased) {
            throw new InvalidOperationException("You can only review products you have purchased");
        }

        Review existing = reviewRepository.findByProductIdAndUserId(productId, user.getId()).orElse(null);

        int oldCount = product.getReviewCount();
        double oldAvg = product.getRating();
        double newRating = request.getRating();

        Review saved;
        if (existing != null) {
            // Editing — swap the old rating out of the running average,
            // new rating in. Count stays the same. Same formula as the
            // verified Firestore version.
            double newAvg = oldCount > 0
                    ? ((oldAvg * oldCount) - existing.getRating() + newRating) / oldCount
                    : newRating;
            existing.setRating(newRating);
            existing.setComment(request.getComment());
            saved = reviewRepository.save(existing);
            product.setRating(round1(newAvg));
            // reviewCount unchanged on edit
        } else {
            int newCount = oldCount + 1;
            double newAvg = ((oldAvg * oldCount) + newRating) / newCount;
            Review review = new Review(product, user, user.getFullName(), newRating, request.getComment());
            saved = reviewRepository.save(review);
            product.setRating(round1(newAvg));
            product.setReviewCount(newCount);
        }

        productRepository.save(product);

        return toDTO(saved);
    }

    public List<ReviewResponseDTO> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(this::toDTO).toList();
    }

    // Matches the verified Firestore version's double.parse(x.toStringAsFixed(1))
    // — round the running average to 1 decimal place before storing.
    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ReviewResponseDTO toDTO(Review review) {
        return new ReviewResponseDTO(review.getId(), review.getUser().getId(), review.getUserName(),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
