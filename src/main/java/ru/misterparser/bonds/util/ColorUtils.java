package ru.misterparser.bonds.util;

import java.math.BigDecimal;

public class ColorUtils {
    
    private static final BigDecimal KEY_RATE = new BigDecimal("18"); // Ключевая ставка 18%
    private static final BigDecimal DOUBLE_KEY_RATE = new BigDecimal("36"); // Двукратное превышение ключевой ставки
    
    public static String getYieldColor(BigDecimal yield) {
        if (yield == null) {
            return "#6c757d"; // Gray for null values
        }
        
        double yieldValue = yield.doubleValue();
        double keyRate = KEY_RATE.doubleValue();
        double doubleKeyRate = DOUBLE_KEY_RATE.doubleValue();
        
        // Если доходность равна ключевой ставке - зеленый цвет
        if (Math.abs(yieldValue - keyRate) < 0.1) {
            return "#00cc00"; // Ярко-зеленый
        }
        
        // Если доходность меньше ключевой ставки - более темный зеленый
        if (yieldValue < keyRate) {
            double ratio = yieldValue / keyRate;
            // От темно-зеленого до ярко-зеленого
            int greenIntensity = (int) (153 + ratio * 51); // от #009900 до #00cc00
            return String.format("#00%02x00", Math.min(greenIntensity, 204));
        }
        
        // Если доходность больше ключевой ставки - градиент от зеленого к бордовому
        if (yieldValue <= doubleKeyRate) {
            // Нормализуем от 0 до 1, где 0 = ключевая ставка, 1 = двукратная ключевая ставка
            double ratio = (yieldValue - keyRate) / (doubleKeyRate - keyRate);
            
            if (ratio <= 0.33) {
                // Зеленый -> Желтый
                int red = (int) (ratio * 3 * 255);
                int green = 204; // Остается зеленым
                return String.format("#%02x%02x00", red, green);
            } else if (ratio <= 0.66) {
                // Желтый -> Красный
                double localRatio = (ratio - 0.33) / 0.33;
                int red = 255;
                int green = (int) (204 * (1 - localRatio));
                return String.format("#%02x%02x00", red, green);
            } else {
                // Красный -> Бордовый
                double localRatio = (ratio - 0.66) / 0.34;
                int red = (int) (255 * (1 - localRatio * 0.5)); // От 255 до 127
                int green = 0;
                int blue = (int) (localRatio * 127); // Добавляем синий для бордового
                return String.format("#%02x%02x%02x", red, green, blue);
            }
        }
        
        // Если доходность больше двукратной ключевой ставки - бордовый
        return "#800020"; // Темно-бордовый
    }
    
    // Для обратной совместимости - оставляем старый метод
    public static String getYieldColor(BigDecimal yield, BigDecimal minYield, BigDecimal maxYield) {
        return getYieldColor(yield);
    }
}