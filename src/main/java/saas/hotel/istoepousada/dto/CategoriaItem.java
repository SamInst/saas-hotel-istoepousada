package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;

public record CategoriaItem(
        @NotNull Long id,
        @NotNull Funcionario.Nome funcionario,
        @NotNull String nome,
        String descricao,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro
){
    public record Id(@NotNull Long id){}
    public record Request(
            @NotNull Funcionario.Id funcionario,
            @NotNull String nome,
            String descricao){
    }

    public static final RowMapper<CategoriaItem> ROW_MAPPER =
            (rs, rowNum) ->
                    new CategoriaItem(
                            rs.getLong("categoria_id"),
                            rs.getObject("categoria_funcionario_id", Long.class) == null
                                    ? null
                                    : new Funcionario.Nome(
                                    rs.getObject("categoria_funcionario_id", Long.class),
                                    rs.getString("categoria_funcionario_nome")),
                            rs.getString("categoria_nome"),
                            rs.getString("categoria_descricao"),
                            rs.getObject("categoria_data_hora_registro", LocalDateTime.class));}
