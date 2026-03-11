package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.RowMapper;
import java.time.LocalDateTime;
import java.util.List;

public record Empresa(
        @NotNull Long id,
        @NotNull LocalDateTime data_hora_registro,
        @NotNull String razao_social,
        String nome_fantasia,
        @NotNull String cnpj,
        @NotNull String telefone,
        @NotNull String email,
        String endereco,
        @NotNull String cep,
        String numero,
        String complemento,
        String pais,
        String estado,
        String municipio,
        String bairro,
        String tipo_empresa,
        Status status,
        Funcionario.Nome funcionario,
        List<Pessoa> pessoas_vinculadas) {
    public record Id(@NotNull Long id) {
    }

    public record Request(
            @NotNull String razao_social,
            String tipo_empresa,
            String nome_fantasia,
            @NotNull String cnpj,
            @NotNull String telefone,
            @NotNull String email,
            String endereco,
            @NotNull String cep,
            String numero,
            String complemento
    ) {}

    public record Update(
            @NotNull Long id,
            @NotNull String cnpj,
            @NotNull String telefone,
            @NotNull String email,
            String endereco,
            @NotNull String cep,
            String numero,
            String complemento,
            String pais,
            String estado,
            String municipio,
            String bairro,
            @NotNull Status status,
            @NotNull String razao_social,
            String nome_fantasia,
            String tipo_empresa
    ){}

    public record Vincular(
            @NotNull Empresa.Id empresa,
            @NotNull Pessoa.Id pessoa,
            Boolean ativo
    ) {}

    public enum Status {
        ATIVO,
        HOSPEDADO,
        BLOQUEADO;
        public static Status map(String status) {
            if (status == null || status.isBlank()) return ATIVO;
            try {return Status.valueOf(status.trim().toUpperCase());}
            catch (IllegalArgumentException ex) {return ATIVO;}
        }
    }

    public static final RowMapper<Empresa> ROW_MAPPER =
            (rs, row_num) ->
                    new Empresa(
                            rs.getLong("id"),
                            rs.getObject("data_hora_registro", LocalDateTime.class),
                            rs.getString("razao_social"),
                            rs.getString("nome_fantasia"),
                            rs.getString("cnpj"),
                            rs.getString("telefone"),
                            rs.getString("email"),
                            rs.getString("endereco"),
                            rs.getString("cep"),
                            rs.getString("numero"),
                            rs.getString("complemento"),
                            rs.getString("pais"),
                            rs.getString("estado"),
                            rs.getString("municipio"),
                            rs.getString("bairro"),
                            rs.getString("tipo_empresa"),
                            Status.map(rs.getString("status")),
                            new Funcionario.Nome(
                                    rs.getObject("funcionario_id", Long.class),
                                    rs.getString("funcionario_nome")
                            ),
                            List.of());

}
