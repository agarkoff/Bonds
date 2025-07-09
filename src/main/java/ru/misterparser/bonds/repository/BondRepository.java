package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.entity.Bond;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
        String sql = "INSERT INTO bonds (ticker, coupon_value, created_at, updated_at) VALUES (?, ?, ?, ?) RETURNING id";
        LocalDateTime now = LocalDateTime.now();
        bond.setCreatedAt(now);
        bond.setUpdatedAt(now);
        
        Long id = jdbcTemplate.queryForObject(sql, Long.class, 
            bond.getTicker(), bond.getCouponValue(), 
            Timestamp.valueOf(bond.getCreatedAt()), 
            Timestamp.valueOf(bond.getUpdatedAt()));
        
        bond.setId(id);
        return bond;
    }
    
    private Bond update(Bond bond) {
        String sql = "UPDATE bonds SET ticker = ?, coupon_value = ?, updated_at = ? WHERE id = ?";
        bond.setUpdatedAt(LocalDateTime.now());
        
        jdbcTemplate.update(sql, bond.getTicker(), bond.getCouponValue(), 
            Timestamp.valueOf(bond.getUpdatedAt()), bond.getId());
        
        return bond;
    }
    
    public void deleteById(Long id) {
        String sql = "DELETE FROM bonds WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    public void upsertBond(String ticker, BigDecimal couponValue) {
        String sql = """
            INSERT INTO bonds (ticker, coupon_value, created_at, updated_at) 
            VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (ticker) 
            DO UPDATE SET 
                coupon_value = EXCLUDED.coupon_value,
                updated_at = CURRENT_TIMESTAMP
            """;
        jdbcTemplate.update(sql, ticker, couponValue);
    }
    
    private static class BondRowMapper implements RowMapper<Bond> {
        @Override
        public Bond mapRow(ResultSet rs, int rowNum) throws SQLException {
            Bond bond = new Bond();
            bond.setId(rs.getLong("id"));
            bond.setTicker(rs.getString("ticker"));
            bond.setCouponValue(rs.getBigDecimal("coupon_value"));
            
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