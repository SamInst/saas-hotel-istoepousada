package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public record Funcionario(
        Long id,
        Pessoa pessoa,
        LocalDate dataAdmissao,
        Objeto cargo
) {

    public static Funcionario mapFuncionario(ResultSet rs, String prefix) throws SQLException {
        return new Funcionario(
                rs.getLong(prefix + "id"),
                Pessoa.mapPessoa(rs, prefix + "pessoa_"),
                rs.getObject(prefix + "data_admissao", LocalDate.class),
                new Objeto(
                        rs.getLong(prefix + "cargo_id"),
                        rs.getString(prefix + "cargo_nome")
                )
        );
    }

}
