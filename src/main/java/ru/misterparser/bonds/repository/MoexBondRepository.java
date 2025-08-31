package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.MoexBond;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MoexBondRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<MoexBond> moexBondRowMapper = new RowMapper<MoexBond>() {
        @Override
        public MoexBond mapRow(ResultSet rs, int rowNum) throws SQLException {
            MoexBond bond = new MoexBond();
            bond.setId(rs.getLong("id"));
            bond.setIsin(rs.getString("isin"));
            bond.setShortName(rs.getString("short_name"));
            bond.setCouponValue(rs.getBigDecimal("coupon_value"));
            bond.setMaturityDate(rs.getDate("maturity_date") != null ? rs.getDate("maturity_date").toLocalDate() : null);
            bond.setFaceValue(rs.getBigDecimal("face_value"));
            bond.setCouponFrequency(rs.getInt("coupon_frequency"));
            bond.setCouponLength(rs.getInt("coupon_length"));
            bond.setCouponDaysPassed(rs.getInt("coupon_days_passed"));
            bond.setOfferDate(rs.getDate("offer_date") != null ? rs.getDate("offer_date").toLocalDate() : null);
            bond.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            bond.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
            return bond;
        }
    };

    /**
     * Сохраняет облигацию МОЭКС (создает новую или обновляет существующую)
     */
    public MoexBond save(MoexBond bond) {
        if (bond.getId() == null) {
            return create(bond);
        } else {
            return update(bond);
        }
    }

    /**
     * Сохраняет или обновляет облигацию по ISIN
     */
    public MoexBond saveOrUpdate(MoexBond bond) {
        Optional<MoexBond> existing = findByIsin(bond.getIsin());
        if (existing.isPresent()) {
            bond.setId(existing.get().getId());
            bond.setCreatedAt(existing.get().getCreatedAt());
            return update(bond);
        } else {
            return create(bond);
        }
    }

    private MoexBond create(MoexBond bond) {
        String sql = "INSERT INTO moex_bonds (isin, short_name, coupon_value, maturity_date, face_value, " +
                    "coupon_frequency, coupon_length, coupon_days_passed, offer_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, bond.getIsin());
            ps.setString(2, bond.getShortName());
            ps.setBigDecimal(3, bond.getCouponValue());
            ps.setDate(4, bond.getMaturityDate() != null ? java.sql.Date.valueOf(bond.getMaturityDate()) : null);
            ps.setBigDecimal(5, bond.getFaceValue());
            ps.setInt(6, bond.getCouponFrequency());
            ps.setInt(7, bond.getCouponLength());
            ps.setInt(8, bond.getCouponDaysPassed());
            ps.setDate(9, bond.getOfferDate() != null ? java.sql.Date.valueOf(bond.getOfferDate()) : null);
            return ps;
        }, keyHolder);

        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("id")) {
            bond.setId(((Number) keys.get("id")).longValue());
        } else {
            Number generatedId = keyHolder.getKey();
            if (generatedId != null) {
                bond.setId(generatedId.longValue());
            }
        }
        
        return findById(bond.getId()).orElse(bond);
    }

    private MoexBond update(MoexBond bond) {
        String sql = "UPDATE moex_bonds SET short_name = ?, coupon_value = ?, maturity_date = ?, " +
                    "face_value = ?, coupon_frequency = ?, coupon_length = ?, coupon_days_passed = ?, " +
                    "offer_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        jdbcTemplate.update(sql, bond.getShortName(), bond.getCouponValue(), 
                          bond.getMaturityDate() != null ? java.sql.Date.valueOf(bond.getMaturityDate()) : null,
                          bond.getFaceValue(), bond.getCouponFrequency(), bond.getCouponLength(),
                          bond.getCouponDaysPassed(), 
                          bond.getOfferDate() != null ? java.sql.Date.valueOf(bond.getOfferDate()) : null,
                          bond.getId());
        
        return findById(bond.getId()).orElse(bond);
    }

    /**
     * Находит облигацию по ID
     */
    public Optional<MoexBond> findById(Long id) {
        String sql = "SELECT * FROM moex_bonds WHERE id = ?";
        List<MoexBond> results = jdbcTemplate.query(sql, moexBondRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Находит облигацию по ISIN
     */
    public Optional<MoexBond> findByIsin(String isin) {
        String sql = "SELECT * FROM moex_bonds WHERE isin = ?";
        List<MoexBond> results = jdbcTemplate.query(sql, moexBondRowMapper, isin);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Возвращает все облигации МОЭКС
     */
    public List<MoexBond> findAll() {
        String sql = "SELECT * FROM moex_bonds ORDER BY isin";
        return jdbcTemplate.query(sql, moexBondRowMapper);
    }

    /**
     * Возвращает количество облигаций в БД
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM moex_bonds";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Удаляет облигацию по ISIN
     */
    public boolean deleteByIsin(String isin) {
        String sql = "DELETE FROM moex_bonds WHERE isin = ?";
        int affected = jdbcTemplate.update(sql, isin);
        return affected > 0;
    }
}
