package saas.hotel.istoepousada.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DefaultConfigRepository {
    private final JdbcTemplate jdbcTemplate;

    public DefaultConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setAbsolutePathFoto(String absolutePath){
        jdbcTemplate.update("update default_config set path_absoluto = 'C:/' where id = 1;", absolutePath);
    }
}
