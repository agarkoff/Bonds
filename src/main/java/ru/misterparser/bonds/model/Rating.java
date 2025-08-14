package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Rating {
    private Long id;
    private String isin;
    private String companyName;
    private String ratingValue;
    private Integer ratingCode;
    private LocalDate ratingDate;
    private LocalDateTime createdAt;

    public Rating(String isin, String ratingValue, LocalDate ratingDate) {
        this.isin = isin;
        this.ratingValue = ratingValue;
        this.ratingDate = ratingDate;
    }
}