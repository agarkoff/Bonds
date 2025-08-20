package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.OfferSubscription;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class OfferSubscriptionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<OfferSubscription> subscriptionRowMapper = new RowMapper<OfferSubscription>() {
        @Override
        public OfferSubscription mapRow(ResultSet rs, int rowNum) throws SQLException {
            OfferSubscription subscription = new OfferSubscription();
            subscription.setId(rs.getLong("id"));
            subscription.setChatId(rs.getLong("chat_id"));
            subscription.setUsername(rs.getString("username"));
            subscription.setIsin(rs.getString("isin"));
            subscription.setTelegramUserId(rs.getObject("telegram_user_id", Long.class));
            subscription.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            subscription.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
            return subscription;
        }
    };

    /**
     * Добавляет подписку на ISIN для пользователя
     */
    public void addSubscription(Long chatId, String username, String isin) {
        String sql = "INSERT INTO offer_subscription (chat_id, username, isin) VALUES (?, ?, ?) " +
                    "ON CONFLICT (chat_id, isin) DO UPDATE SET " +
                    "username = EXCLUDED.username, updated_at = CURRENT_TIMESTAMP";
        jdbcTemplate.update(sql, chatId, username, isin);
    }
    
    /**
     * Добавляет подписку на ISIN для пользователя с telegram_user_id
     */
    public void addSubscription(Long chatId, String username, String isin, Long telegramUserId) {
        String sql = "INSERT INTO offer_subscription (chat_id, username, isin, telegram_user_id) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT (chat_id, isin) DO UPDATE SET " +
                    "username = EXCLUDED.username, telegram_user_id = EXCLUDED.telegram_user_id, updated_at = CURRENT_TIMESTAMP";
        jdbcTemplate.update(sql, chatId, username, isin, telegramUserId);
    }

    /**
     * Удаляет подписку на ISIN для пользователя
     */
    public boolean removeSubscription(Long chatId, String isin) {
        String sql = "DELETE FROM offer_subscription WHERE chat_id = ? AND isin = ?";
        int affected = jdbcTemplate.update(sql, chatId, isin);
        return affected > 0;
    }

    /**
     * Получает все подписки пользователя
     */
    public List<OfferSubscription> findByUserChatId(Long chatId) {
        String sql = "SELECT * FROM offer_subscription WHERE chat_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, subscriptionRowMapper, chatId);
    }

    /**
     * Получает всех пользователей, подписанных на конкретный ISIN
     */
    public List<OfferSubscription> findByIsin(String isin) {
        String sql = "SELECT * FROM offer_subscription WHERE isin = ?";
        return jdbcTemplate.query(sql, subscriptionRowMapper, isin);
    }

    /**
     * Находит подписку по ID
     */
    public Optional<OfferSubscription> findById(Long id) {
        String sql = "SELECT * FROM offer_subscription WHERE id = ?";
        try {
            OfferSubscription subscription = jdbcTemplate.queryForObject(sql, subscriptionRowMapper, id);
            return Optional.ofNullable(subscription);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Проверяет, подписан ли пользователь на ISIN
     */
    public boolean isSubscribed(Long chatId, String isin) {
        String sql = "SELECT COUNT(*) FROM offer_subscription WHERE chat_id = ? AND isin = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, chatId, isin);
        return count != null && count > 0;
    }

    /**
     * Получает все уникальные chat_id пользователей
     */
    public List<Long> findAllChatIds() {
        String sql = "SELECT DISTINCT chat_id FROM offer_subscription ORDER BY chat_id";
        return jdbcTemplate.queryForList(sql, Long.class);
    }

    /**
     * Получает подписки с облигациями, у которых оферта наступает в течение указанного количества дней
     */
    public List<OfferSubscription> findSubscriptionsWithOffersInDays(int days) {
        String sql = "SELECT os.* FROM offer_subscription os " +
                    "JOIN bonds b ON os.isin = b.isin " +
                    "WHERE b.offer_date IS NOT NULL " +
                    "AND b.offer_date > CURRENT_DATE " +
                    "AND b.offer_date <= CURRENT_DATE + INTERVAL '" + days + " days' " +
                    "ORDER BY os.chat_id, b.offer_date";
        return jdbcTemplate.query(sql, subscriptionRowMapper);
    }

    /**
     * Получает количество подписок пользователя
     */
    public int countByUserChatId(Long chatId) {
        String sql = "SELECT COUNT(*) FROM offer_subscription WHERE chat_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, chatId);
        return count != null ? count : 0;
    }

    /**
     * Удаляет все подписки пользователя
     */
    public int removeAllSubscriptionsByUser(Long chatId) {
        String sql = "DELETE FROM offer_subscription WHERE chat_id = ?";
        return jdbcTemplate.update(sql, chatId);
    }
}