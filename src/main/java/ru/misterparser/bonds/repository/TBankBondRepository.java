package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.TBankBond;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class TBankBondRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<TBankBond> rowMapper = new RowMapper<TBankBond>() {
        @Override
        public TBankBond mapRow(ResultSet rs, int rowNum) throws SQLException {
            TBankBond bond = new TBankBond();
            bond.setInstrumentUid(rs.getString("instrument_uid"));
            bond.setFigi(rs.getString("figi"));
            bond.setTicker(rs.getString("ticker"));
            bond.setAssetUid(rs.getString("asset_uid"));
            bond.setBrandName(rs.getString("brand_name"));
            return bond;
        }
    };

    public void saveOrUpdate(TBankBond bond) {
        try {
            String sql = "INSERT INTO tbank_bonds (instrument_uid, figi, ticker, asset_uid, brand_name) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT (instrument_uid) " +
                    "DO UPDATE SET " +
                    "figi = EXCLUDED.figi, " +
                    "ticker = EXCLUDED.ticker, " +
                    "asset_uid = EXCLUDED.asset_uid, " +
                    "brand_name = EXCLUDED.brand_name, " +
                    "updated_at = CURRENT_TIMESTAMP";
            
            jdbcTemplate.update(sql,
                bond.getInstrumentUid(),
                bond.getFigi(),
                bond.getTicker(),
                bond.getAssetUid(),
                bond.getBrandName()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save or update T-Bank bond: " + bond.getInstrumentUid(), e);
        }
    }

    public Optional<TBankBond> findByInstrumentUid(String instrumentUid) {
        try {
            String sql = "SELECT * FROM tbank_bonds WHERE instrument_uid = ?";
            List<TBankBond> bonds = jdbcTemplate.query(sql, rowMapper, instrumentUid);
            return bonds.isEmpty() ? Optional.empty() : Optional.of(bonds.get(0));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<TBankBond> findByFigi(String figi) {
        try {
            String sql = "SELECT * FROM tbank_bonds WHERE figi = ?";
            List<TBankBond> bonds = jdbcTemplate.query(sql, rowMapper, figi);
            return bonds.isEmpty() ? Optional.empty() : Optional.of(bonds.get(0));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    public List<TBankBond> findAll() {
        try {
            String sql = "SELECT * FROM tbank_bonds ORDER BY ticker";
            return jdbcTemplate.query(sql, rowMapper);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to load T-Bank bonds", e);
        }
    }

    public void deleteAll() {
        try {
            jdbcTemplate.update("DELETE FROM tbank_bonds");
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to delete T-Bank bonds", e);
        }
    }
}