package ru.misterparser.bonds.util;

import java.util.HashMap;
import java.util.Map;

public class RatingUtils {
    
    private static final Map<String, Integer> RATING_CODES = new HashMap<>();
    
    static {
        RATING_CODES.put("ruAAA", 48);
        RATING_CODES.put("ruAA+", 49);
        RATING_CODES.put("ruAA", 50);
        RATING_CODES.put("ruAA-", 51);
        RATING_CODES.put("ruA+", 52);
        RATING_CODES.put("ruA", 53);
        RATING_CODES.put("ruA-", 54);
        RATING_CODES.put("ruBBB+", 146);
        RATING_CODES.put("ruBBB", 147);
        RATING_CODES.put("ruBBB-", 148);
        RATING_CODES.put("ruBB+", 149);
        RATING_CODES.put("ruBB", 150);
        RATING_CODES.put("ruBB-", 151);
        RATING_CODES.put("ruB+", 152);
        RATING_CODES.put("ruB", 153);
        RATING_CODES.put("ruB-", 154);
        RATING_CODES.put("ruCCC", 249);
        RATING_CODES.put("ruCC", 250);
        RATING_CODES.put("ruC", 251);
        RATING_CODES.put("ruRD", 252);
        RATING_CODES.put("ruD", 253);
    }
    
    public static Integer getRatingCode(String ratingValue) {
        return RATING_CODES.get(ratingValue);
    }
    
    public static boolean isValidRating(String ratingValue) {
        return RATING_CODES.containsKey(ratingValue);
    }
}