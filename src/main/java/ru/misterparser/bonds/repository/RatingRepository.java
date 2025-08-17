package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.Rating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class RatingRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Rating> ratingRowMapper = new RowMapper<Rating>() {
        @Override
        public Rating mapRow(ResultSet rs, int rowNum) throws SQLException {
            Rating rating = new Rating();
            rating.setId(rs.getLong("id"));
            rating.setIsin(rs.getString("isin"));
            rating.setCompanyName(rs.getString("company_name"));
            rating.setRatingValue(rs.getString("rating_value"));
            Integer ratingCode = rs.getObject("rating_code", Integer.class);
            rating.setRatingCode(ratingCode);
            rating.setRatingDate(rs.getDate("rating_date") != null ? rs.getDate("rating_date").toLocalDate() : null);
            rating.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            return rating;
        }
    };

    public List<Rating> findAll() {
        return jdbcTemplate.query("SELECT * FROM ratings ORDER BY rating_date DESC", ratingRowMapper);
    }

    public List<Rating> findByIsin(String isin) {
        return jdbcTemplate.query("SELECT * FROM ratings WHERE isin = ? ORDER BY rating_date DESC", 
                ratingRowMapper, isin);
    }

    public void save(Rating rating) {
        String sql = "INSERT INTO ratings (isin, company_name, rating_value, rating_code, rating_date) " +
                "VALUES (?, ?, ?, ?, ?) ON CONFLICT (isin, rating_date) DO UPDATE SET " +
                "company_name = EXCLUDED.company_name, rating_value = EXCLUDED.rating_value, " +
                "rating_code = EXCLUDED.rating_code";
        
        jdbcTemplate.update(sql,
                rating.getIsin(),
                rating.getCompanyName(),
                rating.getRatingValue(),
                rating.getRatingCode(),
                rating.getRatingDate()
        );
    }

    public boolean existsByIsinAndDate(String isin, java.time.LocalDate date) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ratings WHERE isin = ? AND rating_date = ?",
                Long.class, isin, date);
        return count != null && count > 0;
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM ratings");
    }
}