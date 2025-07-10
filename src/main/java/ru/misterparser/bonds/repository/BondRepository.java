package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.entity.Bond;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class BondRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<Bond> bondRowMapper = new BondRowMapper();
    
    public List<Bond> findAll() {
        String sql = "SELECT * FROM bonds ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, bondRowMapper);
    }
    
    public Optional<Bond> findById(Long id) {
        String sql = "SELECT * FROM bonds WHERE id = ?";
        List<Bond> bonds = jdbcTemplate.query(sql, bondRowMapper, id);
        return bonds.isEmpty() ? Optional.empty() : Optional.of(bonds.get(0));
    }
    
    public Optional<Bond> findByTicker(String ticker) {
        String sql = "SELECT * FROM bonds WHERE ticker = ?";
        List<Bond> bonds = jdbcTemplate.query(sql, bondRowMapper, ticker);
        return bonds.isEmpty() ? Optional.empty() : Optional.of(bonds.get(0));
    }
    
    public boolean existsByTicker(String ticker) {
        String sql = "SELECT COUNT(*) FROM bonds WHERE ticker = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, ticker);
        return count != null && count > 0;
    }
    
    public Bond save(Bond bond) {
        if (bond.getId() == null) {
            return insert(bond);
        } else {
            return update(bond);
        }
    }
    
    private Bond insert(Bond bond) {
        String sql = "INSERT INTO bonds (ticker, coupon_value, maturity_date, wa_price, face_value, coupon_frequency, coupon_length, nkd, fee, profit, net_profit, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        LocalDateTime now = LocalDateTime.now();
        bond.setCreatedAt(now);
        bond.setUpdatedAt(now);
        
        Long id = jdbcTemplate.queryForObject(sql, Long.class, 
            bond.getTicker(), bond.getCouponValue(), 
            bond.getMaturityDate() != null ? Date.valueOf(bond.getMaturityDate()) : null,
            bond.getWaPrice(),
            bond.getFaceValue(),
            bond.getCouponFrequency(),
            bond.getCouponLength(),
            bond.getNkd(),
            bond.getFee(),
            bond.getProfit(),
            bond.getNetProfit(),
            Timestamp.valueOf(bond.getCreatedAt()), 
            Timestamp.valueOf(bond.getUpdatedAt()));
        
        bond.setId(id);
        return bond;
    }
    
    private Bond update(Bond bond) {
        String sql = "UPDATE bonds SET ticker = ?, coupon_value = ?, maturity_date = ?, wa_price = ?, face_value = ?, coupon_frequency = ?, coupon_length = ?, nkd = ?, fee = ?, profit = ?, net_profit = ?, updated_at = ? WHERE id = ?";
        bond.setUpdatedAt(LocalDateTime.now());
        
        jdbcTemplate.update(sql, bond.getTicker(), bond.getCouponValue(), 
            bond.getMaturityDate() != null ? Date.valueOf(bond.getMaturityDate()) : null,
            bond.getWaPrice(),
            bond.getFaceValue(),
            bond.getCouponFrequency(),
            bond.getCouponLength(),
            bond.getNkd(),
            bond.getFee(),
            bond.getProfit(),
            bond.getNetProfit(),
            Timestamp.valueOf(bond.getUpdatedAt()), bond.getId());
        
        return bond;
    }
    
    public void deleteById(Long id) {
        String sql = "DELETE FROM bonds WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    public void upsertBond(String ticker, BigDecimal couponValue, LocalDate maturityDate, BigDecimal waPrice, BigDecimal faceValue, Integer couponFrequency) {
        String sql = """
            INSERT INTO bonds (ticker, coupon_value, maturity_date, wa_price, face_value, coupon_frequency, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (ticker)
            DO UPDATE SET
                coupon_value = EXCLUDED.coupon_value,
                maturity_date = EXCLUDED.maturity_date,
                wa_price = EXCLUDED.wa_price,
                face_value = EXCLUDED.face_value,
                coupon_frequency = EXCLUDED.coupon_frequency,
                updated_at = CURRENT_TIMESTAMP
            """;
        jdbcTemplate.update(sql, ticker, couponValue, maturityDate != null ? Date.valueOf(maturityDate) : null, waPrice, faceValue, couponFrequency);
    }
    
    public void upsertBond(String ticker, BigDecimal couponValue, LocalDate maturityDate, BigDecimal waPrice, BigDecimal faceValue, Integer couponFrequency, Integer couponLength, BigDecimal nkd, BigDecimal fee, BigDecimal profit, BigDecimal netProfit) {
        String sql = """
            INSERT INTO bonds (ticker, coupon_value, maturity_date, wa_price, face_value, coupon_frequency, coupon_length, nkd, fee, profit, net_profit, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (ticker)
            DO UPDATE SET
                coupon_value = EXCLUDED.coupon_value,
                maturity_date = EXCLUDED.maturity_date,
                wa_price = EXCLUDED.wa_price,
                face_value = EXCLUDED.face_value,
                coupon_frequency = EXCLUDED.coupon_frequency,
                coupon_length = EXCLUDED.coupon_length,
                nkd = EXCLUDED.nkd,
                fee = EXCLUDED.fee,
                profit = EXCLUDED.profit,
                net_profit = EXCLUDED.net_profit,
                updated_at = CURRENT_TIMESTAMP
            """;
        jdbcTemplate.update(sql, ticker, couponValue, maturityDate != null ? Date.valueOf(maturityDate) : null, waPrice, faceValue, couponFrequency, couponLength, nkd, fee, profit, netProfit);
    }
    
    private static class BondRowMapper implements RowMapper<Bond> {
        @Override
        public Bond mapRow(ResultSet rs, int rowNum) throws SQLException {
            Bond bond = new Bond();
            bond.setId(rs.getLong("id"));
            bond.setTicker(rs.getString("ticker"));
            bond.setCouponValue(rs.getBigDecimal("coupon_value"));
            
            Date maturityDate = rs.getDate("maturity_date");
            if (maturityDate != null) {
                bond.setMaturityDate(maturityDate.toLocalDate());
            }
            
            bond.setWaPrice(rs.getBigDecimal("wa_price"));
            bond.setFaceValue(rs.getBigDecimal("face_value"));
            bond.setCouponFrequency(rs.getInt("coupon_frequency"));
            if (rs.wasNull()) {
                bond.setCouponFrequency(null);
            }
            bond.setCouponLength(rs.getInt("coupon_length"));
            if (rs.wasNull()) {
                bond.setCouponLength(null);
            }
            bond.setNkd(rs.getBigDecimal("nkd"));
            bond.setFee(rs.getBigDecimal("fee"));
            bond.setProfit(rs.getBigDecimal("profit"));
            bond.setNetProfit(rs.getBigDecimal("net_profit"));
            
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                bond.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                bond.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            
            return bond;
        }
    }
}