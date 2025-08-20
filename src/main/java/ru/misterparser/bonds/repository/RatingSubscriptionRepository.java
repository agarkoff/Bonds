package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.RatingSubscription;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RatingSubscriptionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<RatingSubscription> subscriptionRowMapper = new RowMapper<RatingSubscription>() {
        @Override
        public RatingSubscription mapRow(ResultSet rs, int rowNum) throws SQLException {
            RatingSubscription subscription = new RatingSubscription();
            subscription.setId(rs.getLong("id"));
            subscription.setTelegramUserId(rs.getLong("telegram_user_id"));
            subscription.setName(rs.getString("name"));
            subscription.setPeriodHours(rs.getInt("period_hours"));
            subscription.setMinYield(rs.getBigDecimal("min_yield"));
            subscription.setMaxYield(rs.getBigDecimal("max_yield"));
            subscription.setTickerCount(rs.getInt("ticker_count"));
            subscription.setIncludeOffer(rs.getBoolean("include_offer"));
            subscription.setMinMaturityWeeks(rs.getObject("min_maturity_weeks", Integer.class));
            subscription.setMaxMaturityWeeks(rs.getObject("max_maturity_weeks", Integer.class));
            subscription.setEnabled(rs.getBoolean("enabled"));
            subscription.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            subscription.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
            subscription.setLastSentAt(rs.getTimestamp("last_sent_at") != null ? rs.getTimestamp("last_sent_at").toLocalDateTime() : null);
            return subscription;
        }
    };

    /**
     * Создает новую подписку на рейтинг
     */
    public RatingSubscription save(RatingSubscription subscription) {
        if (subscription.getId() == null) {
            return create(subscription);
        } else {
            return update(subscription);
        }
    }

    private RatingSubscription create(RatingSubscription subscription) {
        String sql = "INSERT INTO rating_subscription (telegram_user_id, name, period_hours, min_yield, max_yield, " +
                    "ticker_count, include_offer, min_maturity_weeks, max_maturity_weeks, enabled) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, subscription.getTelegramUserId());
            ps.setString(2, subscription.getName());
            ps.setInt(3, subscription.getPeriodHours());
            ps.setBigDecimal(4, subscription.getMinYield());
            ps.setBigDecimal(5, subscription.getMaxYield());
            ps.setInt(6, subscription.getTickerCount());
            ps.setBoolean(7, subscription.isIncludeOffer());
            ps.setObject(8, subscription.getMinMaturityWeeks());
            ps.setObject(9, subscription.getMaxMaturityWeeks());
            ps.setBoolean(10, subscription.isEnabled());
            return ps;
        }, keyHolder);
        
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("id")) {
            subscription.setId(((Number) keys.get("id")).longValue());
        } else {
            Number generatedId = keyHolder.getKey();
            if (generatedId != null) {
                subscription.setId(generatedId.longValue());
            }
        }
        return findById(subscription.getId()).orElse(subscription);
    }

    private RatingSubscription update(RatingSubscription subscription) {
        String sql = "UPDATE rating_subscription SET name = ?, period_hours = ?, min_yield = ?, max_yield = ?, " +
                    "ticker_count = ?, include_offer = ?, min_maturity_weeks = ?, max_maturity_weeks = ?, " +
                    "enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        jdbcTemplate.update(sql, subscription.getName(), subscription.getPeriodHours(), 
                          subscription.getMinYield(), subscription.getMaxYield(), subscription.getTickerCount(),
                          subscription.isIncludeOffer(), subscription.getMinMaturityWeeks(), 
                          subscription.getMaxMaturityWeeks(), subscription.isEnabled(), subscription.getId());
        
        return findById(subscription.getId()).orElse(subscription);
    }

    /**
     * Находит подписку по ID
     */
    public Optional<RatingSubscription> findById(Long id) {
        String sql = "SELECT * FROM rating_subscription WHERE id = ?";
        List<RatingSubscription> results = jdbcTemplate.query(sql, subscriptionRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Находит все подписки пользователя
     */
    public List<RatingSubscription> findByTelegramUserId(Long telegramUserId) {
        String sql = "SELECT * FROM rating_subscription WHERE telegram_user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, subscriptionRowMapper, telegramUserId);
    }

    /**
     * Находит все активные подписки
     */
    public List<RatingSubscription> findAllEnabled() {
        String sql = "SELECT * FROM rating_subscription WHERE enabled = true ORDER BY telegram_user_id, id";
        return jdbcTemplate.query(sql, subscriptionRowMapper);
    }

    /**
     * Находит подписки, которые должны быть отправлены
     */
    public List<RatingSubscription> findSubscriptionsToSend() {
        String sql = "SELECT * FROM rating_subscription WHERE enabled = true AND " +
                    "(last_sent_at IS NULL OR last_sent_at <= CURRENT_TIMESTAMP - INTERVAL '1 hour' * period_hours) " +
                    "ORDER BY telegram_user_id, id";
        return jdbcTemplate.query(sql, subscriptionRowMapper);
    }

    /**
     * Обновляет время последней отправки подписки
     */
    public void updateLastSentAt(Long subscriptionId, LocalDateTime sentAt) {
        String sql = "UPDATE rating_subscription SET last_sent_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, sentAt, subscriptionId);
    }

    /**
     * Обновляет название подписки
     */
    public void updateName(Long subscriptionId, String newName) {
        String sql = "UPDATE rating_subscription SET name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        jdbcTemplate.update(sql, newName, subscriptionId);
    }

    /**
     * Удаляет подписку
     */
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM rating_subscription WHERE id = ?";
        int affected = jdbcTemplate.update(sql, id);
        return affected > 0;
    }

    /**
     * Удаляет все подписки пользователя
     */
    public int deleteByTelegramUserId(Long telegramUserId) {
        String sql = "DELETE FROM rating_subscription WHERE telegram_user_id = ?";
        return jdbcTemplate.update(sql, telegramUserId);
    }

    /**
     * Подсчитывает количество подписок пользователя
     */
    public int countByTelegramUserId(Long telegramUserId) {
        String sql = "SELECT COUNT(*) FROM rating_subscription WHERE telegram_user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, telegramUserId);
        return count != null ? count : 0;
    }

    /**
     * Включает/выключает подписку
     */
    public void setEnabled(Long subscriptionId, boolean enabled) {
        String sql = "UPDATE rating_subscription SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        jdbcTemplate.update(sql, enabled, subscriptionId);
    }
}