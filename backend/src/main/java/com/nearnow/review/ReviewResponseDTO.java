package com.nearnow.review;

import java.time.Instant;

public class ReviewResponseDTO {

    private Long id;
    private Long userId;
    private String userName;
    private double rating;
    private String comment;
    private Instant createdAt;

    public ReviewResponseDTO(Long id, Long userId, String userName, double rating,
                              String comment, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public double getRating() { return rating; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
