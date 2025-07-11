package ru.misterparser.bonds.util;

import java.math.BigDecimal;

public class ColorUtils {
    
    /**
     * Генерирует цвет градиента от красного к зеленому на основе значения доходности
     * @param yield текущая доходность
     * @param minYield минимальная доходность в наборе данных
     * @param maxYield максимальная доходность в наборе данных
     * @return HEX цвет для CSS
     */
    public static String getYieldColor(BigDecimal yield, BigDecimal minYield, BigDecimal maxYield) {
        if (yield == null || minYield == null || maxYield == null) {
            return "#666666";
        }
        
        // Избегаем деления на ноль
        BigDecimal range = maxYield.subtract(minYield);
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return "#4caf50"; // Зеленый, если все значения одинаковые
        }
        
        // Нормализуем значение от 0 до 1
        double normalizedValue = yield.subtract(minYield)
                .divide(range, 4, BigDecimal.ROUND_HALF_UP)
                .doubleValue();
        
        // Ограничиваем значение от 0 до 1
        normalizedValue = Math.max(0, Math.min(1, normalizedValue));
        
        // Генерируем градиент от красного (низкая доходность) к зеленому (высокая доходность)
        int red, green, blue;
        
        if (normalizedValue < 0.5) {
            // От красного к желтому
            red = 255;
            green = (int) (255 * normalizedValue * 2);
            blue = 0;
        } else {
            // От желтого к зеленому
            red = (int) (255 * (1 - normalizedValue) * 2);
            green = 255;
            blue = 0;
        }
        
        // Добавляем немного синего для более приятного вида
        blue = Math.min(100, (int) (normalizedValue * 50));
        
        return String.format("#%02x%02x%02x", red, green, blue);
    }
}