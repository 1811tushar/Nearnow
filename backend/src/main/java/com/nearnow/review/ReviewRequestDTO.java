package com.nearnow.review;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Deliberately no productId/userId fields — productId comes from the
 * URL path, userId from the JWT (Authentication object), never from
 * the request body. Same "never trust the client for identity/ownership
 * data it could forge" discipline as everywhere else in this project.
 */
public class ReviewRequestDTO {

    @NotNull(message = "Rating is required")
    @DecimalMin(value = "1.0", message = "Rating must be at least 1")
    @DecimalMax(value = "5.0", message = "Rating must be at most 5")
    private Double rating;

    private String comment;

    public ReviewRequestDTO() {}

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
