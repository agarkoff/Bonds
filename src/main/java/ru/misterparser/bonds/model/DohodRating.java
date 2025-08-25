package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DohodRating {
    private Long id;
    private String isin;
    private String ratingValue;
    private Integer ratingCode;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    public DohodRating(String isin, String ratingValue, Integer ratingCode) {
        this.isin = isin;
        this.ratingValue = ratingValue;
        this.ratingCode = ratingCode;
    }
}