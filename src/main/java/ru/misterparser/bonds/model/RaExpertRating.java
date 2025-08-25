package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RaExpertRating {
    private Long id;
    private String isin;
    private String companyName;
    private String ratingValue;
    private Integer ratingCode;
    private LocalDate ratingDate;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    public RaExpertRating(String isin, String companyName, String ratingValue, Integer ratingCode, LocalDate ratingDate) {
        this.isin = isin;
        this.companyName = companyName;
        this.ratingValue = ratingValue;
        this.ratingCode = ratingCode;
        this.ratingDate = ratingDate;
    }
}