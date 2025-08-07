package ru.misterparser.bonds.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Rating {
    private Long id;
    private String isin;
    private String companyName;
    private String ratingValue;
    private Integer ratingCode;
    private LocalDate ratingDate;
    private LocalDateTime createdAt;

    public Rating() {}

    public Rating(String isin, String ratingValue, LocalDate ratingDate) {
        this.isin = isin;
        this.ratingValue = ratingValue;
        this.ratingDate = ratingDate;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getRatingValue() {
        return ratingValue;
    }

    public void setRatingValue(String ratingValue) {
        this.ratingValue = ratingValue;
    }

    public Integer getRatingCode() {
        return ratingCode;
    }

    public void setRatingCode(Integer ratingCode) {
        this.ratingCode = ratingCode;
    }

    public LocalDate getRatingDate() {
        return ratingDate;
    }

    public void setRatingDate(LocalDate ratingDate) {
        this.ratingDate = ratingDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}