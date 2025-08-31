package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.DohodRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class DohodRatingRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<DohodRating> rowMapper = new RowMapper<DohodRating>() {
        @Override
        public DohodRating mapRow(ResultSet rs, int rowNum) throws SQLException {
            DohodRating rating = new DohodRating();
            rating.setId(rs.getLong("id"));
            rating.setIsin(rs.getString("isin"));
            rating.setRatingValue(rs.getString("rating_value"));
            rating.setRatingCode(rs.getInt("rating_code"));
            rating.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            rating.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return rating;
        }
    };

    public void saveOrUpdate(DohodRating rating) {
        try {
            String sql = "INSERT INTO dohod_ratings (isin, rating_value, rating_code) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT (isin) " +
                    "DO UPDATE SET " +
                    "rating_value = EXCLUDED.rating_value, " +
                    "rating_code = EXCLUDED.rating_code, " +
                    "updated_at = CURRENT_TIMESTAMP";
            
            jdbcTemplate.update(sql,
                rating.getIsin(),
                rating.getRatingValue(),
                rating.getRatingCode()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save or update Dohod rating for ISIN: " + rating.getIsin(), e);
        }
    }

    public List<DohodRating> findAll() {
        try {
            String sql = "SELECT * FROM dohod_ratings ORDER BY isin";
            return jdbcTemplate.query(sql, rowMapper);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to load Dohod ratings", e);
        }
    }
}
