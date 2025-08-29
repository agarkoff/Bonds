package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.UserFilterSettings;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserFilterSettingsRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserFilterSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Найти настройки фильтров пользователя по его ID
     */
    public Optional<UserFilterSettings> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, limit_value, weeks_to_maturity, fee_percent, " +
                     "yield_range, search_text, show_offer, selected_ratings, " +
                     "created_at, updated_at " +
                     "FROM user_filter_settings WHERE user_id = ?";
        
        try {
            UserFilterSettings settings = jdbcTemplate.queryForObject(sql, new UserFilterSettingsRowMapper(), userId);
            return Optional.ofNullable(settings);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Сохранить или обновить настройки фильтров пользователя
     */
    public void saveOrUpdate(UserFilterSettings settings) {
        if (settings.getId() == null) {
            insert(settings);
        } else {
            update(settings);
        }
    }

    /**
     * Вставить новые настройки
     */
    private void insert(UserFilterSettings settings) {
        String sql = "INSERT INTO user_filter_settings " +
                     "(user_id, limit_value, weeks_to_maturity, fee_percent, yield_range, " +
                     "search_text, show_offer, selected_ratings, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        LocalDateTime now = LocalDateTime.now();
        settings.setCreatedAt(now);
        settings.setUpdatedAt(now);
        
        jdbcTemplate.update(sql,
            settings.getUserId(),
            settings.getLimit(),
            settings.getWeeksToMaturity(),
            settings.getFeePercent(),
            settings.getYieldRange(),
            settings.getSearchText(),
            settings.getShowOffer(),
            settings.getSelectedRatings(),
            settings.getCreatedAt(),
            settings.getUpdatedAt()
        );
    }

    /**
     * Обновить существующие настройки
     */
    private void update(UserFilterSettings settings) {
        String sql = "UPDATE user_filter_settings SET " +
                     "limit_value = ?, weeks_to_maturity = ?, fee_percent = ?, " +
                     "yield_range = ?, search_text = ?, show_offer = ?, " +
                     "selected_ratings = ?, updated_at = ? " +
                     "WHERE id = ?";
        
        settings.setUpdatedAt(LocalDateTime.now());
        
        jdbcTemplate.update(sql,
            settings.getLimit(),
            settings.getWeeksToMaturity(),
            settings.getFeePercent(),
            settings.getYieldRange(),
            settings.getSearchText(),
            settings.getShowOffer(),
            settings.getSelectedRatings(),
            settings.getUpdatedAt(),
            settings.getId()
        );
    }

    /**
     * RowMapper для преобразования результата запроса в объект UserFilterSettings
     */
    private static class UserFilterSettingsRowMapper implements RowMapper<UserFilterSettings> {
        @Override
        public UserFilterSettings mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserFilterSettings settings = new UserFilterSettings();
            settings.setId(rs.getLong("id"));
            settings.setUserId(rs.getLong("user_id"));
            settings.setLimit(rs.getInt("limit_value"));
            settings.setWeeksToMaturity(rs.getString("weeks_to_maturity"));
            settings.setFeePercent(rs.getDouble("fee_percent"));
            settings.setYieldRange(rs.getString("yield_range"));
            settings.setSearchText(rs.getString("search_text"));
            settings.setShowOffer(rs.getBoolean("show_offer"));
            settings.setSelectedRatings(rs.getString("selected_ratings"));
            
            // Обработка временных полей
            if (rs.getTimestamp("created_at") != null) {
                settings.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                settings.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
            
            return settings;
        }
    }
}