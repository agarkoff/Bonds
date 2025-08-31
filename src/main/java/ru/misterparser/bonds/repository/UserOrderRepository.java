package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.UserOrder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserOrderRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<UserOrder> orderRowMapper = new RowMapper<UserOrder>() {
        @Override
        public UserOrder mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserOrder order = new UserOrder();
            order.setId(rs.getLong("id"));
            order.setTelegramUserId(rs.getLong("telegram_user_id"));
            order.setPurchaseDate(rs.getDate("purchase_date") != null ? rs.getDate("purchase_date").toLocalDate() : null);
            order.setIsin(rs.getString("isin"));
            order.setTicker(rs.getString("ticker"));
            order.setBondName(rs.getString("bond_name"));
            order.setRating(rs.getString("rating"));
            order.setCouponValue(rs.getBigDecimal("coupon_value"));
            order.setCouponPeriod(rs.getInt("coupon_period"));
            order.setMaturityDate(rs.getDate("maturity_date") != null ? rs.getDate("maturity_date").toLocalDate() : null);
            order.setPriceAsk(rs.getBigDecimal("price_ask"));
            order.setNkd(rs.getBigDecimal("nkd"));
            order.setFeePercent(rs.getBigDecimal("fee_percent"));
            order.setTotalCosts(rs.getBigDecimal("total_costs"));
            order.setFaceValue(rs.getBigDecimal("face_value"));
            order.setTotalCoupon(rs.getBigDecimal("total_coupon"));
            order.setTotalIncome(rs.getBigDecimal("total_income"));
            order.setNetProfit(rs.getBigDecimal("net_profit"));
            order.setAnnualYield(rs.getBigDecimal("annual_yield"));
            
            if (rs.getTimestamp("created_at") != null) {
                order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
            
            return order;
        }
    };

    /**
     * Создать новую сделку
     */
    public UserOrder save(UserOrder order) {
        if (order.getId() == null) {
            return create(order);
        } else {
            return update(order);
        }
    }

    private UserOrder create(UserOrder order) {
        String sql = "INSERT INTO user_orders " +
                     "(telegram_user_id, purchase_date, isin, ticker, bond_name, rating, " +
                     "coupon_value, coupon_period, maturity_date, price_ask, nkd, fee_percent, " +
                     "total_costs, face_value, total_coupon, total_income, net_profit, annual_yield, " +
                     "created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, order.getTelegramUserId());
            ps.setDate(2, order.getPurchaseDate() != null ? java.sql.Date.valueOf(order.getPurchaseDate()) : null);
            ps.setString(3, order.getIsin());
            ps.setString(4, order.getTicker());
            ps.setString(5, order.getBondName());
            ps.setString(6, order.getRating());
            ps.setBigDecimal(7, order.getCouponValue());
            ps.setObject(8, order.getCouponPeriod());
            ps.setDate(9, order.getMaturityDate() != null ? java.sql.Date.valueOf(order.getMaturityDate()) : null);
            ps.setBigDecimal(10, order.getPriceAsk());
            ps.setBigDecimal(11, order.getNkd());
            ps.setBigDecimal(12, order.getFeePercent());
            ps.setBigDecimal(13, order.getTotalCosts());
            ps.setBigDecimal(14, order.getFaceValue());
            ps.setBigDecimal(15, order.getTotalCoupon());
            ps.setBigDecimal(16, order.getTotalIncome());
            ps.setBigDecimal(17, order.getNetProfit());
            ps.setBigDecimal(18, order.getAnnualYield());
            ps.setTimestamp(19, java.sql.Timestamp.valueOf(order.getCreatedAt()));
            ps.setTimestamp(20, java.sql.Timestamp.valueOf(order.getUpdatedAt()));
            return ps;
        }, keyHolder);

        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("id")) {
            order.setId(((Number) keys.get("id")).longValue());
        } else {
            Number generatedId = keyHolder.getKey();
            if (generatedId != null) {
                order.setId(generatedId.longValue());
            }
        }

        return order;
    }

    private UserOrder update(UserOrder order) {
        String sql = "UPDATE user_orders SET " +
                     "purchase_date = ?, isin = ?, ticker = ?, bond_name = ?, rating = ?, " +
                     "coupon_value = ?, coupon_period = ?, maturity_date = ?, price_ask = ?, nkd = ?, " +
                     "fee_percent = ?, total_costs = ?, face_value = ?, total_coupon = ?, " +
                     "total_income = ?, net_profit = ?, annual_yield = ?, updated_at = ? " +
                     "WHERE id = ?";

        order.setUpdatedAt(LocalDateTime.now());

        jdbcTemplate.update(sql,
            order.getPurchaseDate() != null ? java.sql.Date.valueOf(order.getPurchaseDate()) : null,
            order.getIsin(),
            order.getTicker(),
            order.getBondName(),
            order.getRating(),
            order.getCouponValue(),
            order.getCouponPeriod(),
            order.getMaturityDate() != null ? java.sql.Date.valueOf(order.getMaturityDate()) : null,
            order.getPriceAsk(),
            order.getNkd(),
            order.getFeePercent(),
            order.getTotalCosts(),
            order.getFaceValue(),
            order.getTotalCoupon(),
            order.getTotalIncome(),
            order.getNetProfit(),
            order.getAnnualYield(),
            java.sql.Timestamp.valueOf(order.getUpdatedAt()),
            order.getId()
        );

        return order;
    }

    /**
     * Найти сделку по ID
     */
    public Optional<UserOrder> findById(Long id) {
        String sql = "SELECT * FROM user_orders WHERE id = ?";
        List<UserOrder> results = jdbcTemplate.query(sql, orderRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Найти все сделки пользователя
     */
    public List<UserOrder> findByTelegramUserId(Long telegramUserId) {
        String sql = "SELECT * FROM user_orders WHERE telegram_user_id = ? ORDER BY purchase_date DESC, created_at DESC";
        return jdbcTemplate.query(sql, orderRowMapper, telegramUserId);
    }

    /**
     * Удалить сделку
     */
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM user_orders WHERE id = ?";
        int affected = jdbcTemplate.update(sql, id);
        return affected > 0;
    }

    /**
     * Получить количество сделок пользователя
     */
    public int countByTelegramUserId(Long telegramUserId) {
        String sql = "SELECT COUNT(*) FROM user_orders WHERE telegram_user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, telegramUserId);
        return count != null ? count : 0;
    }
}