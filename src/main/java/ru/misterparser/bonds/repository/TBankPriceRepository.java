package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.TBankPrice;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class TBankPriceRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<TBankPrice> rowMapper = new RowMapper<TBankPrice>() {
        @Override
        public TBankPrice mapRow(ResultSet rs, int rowNum) throws SQLException {
            TBankPrice price = new TBankPrice();
            price.setFigi(rs.getString("figi"));
            price.setPriceAsk(rs.getBigDecimal("price_ask"));
            price.setPriceBid(rs.getBigDecimal("price_bid"));
            return price;
        }
    };

    public void saveOrUpdate(TBankPrice price) {
        try {
            String sql = "INSERT INTO tbank_prices (figi, price_ask, price_bid) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT (figi) " +
                    "DO UPDATE SET " +
                    "price_ask = EXCLUDED.price_ask, " +
                    "price_bid = EXCLUDED.price_bid, " +
                    "updated_at = CURRENT_TIMESTAMP";
            
            jdbcTemplate.update(sql,
                price.getFigi(),
                price.getPriceAsk(),
                price.getPriceBid()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save or update T-Bank price: " + price.getFigi(), e);
        }
    }

    public List<TBankPrice> findAll() {
        try {
            String sql = "SELECT * FROM tbank_prices ORDER BY figi";
            return jdbcTemplate.query(sql, rowMapper);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to load T-Bank prices", e);
        }
    }
}