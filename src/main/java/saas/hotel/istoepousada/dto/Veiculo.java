package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.RowMapper;

public record Veiculo(
        @NotNull Long id,
        String modelo,
        String marca,
        Integer ano,
        @NotNull String placa,
        String cor) {
    public record Id(@NotNull Long id) {}
    public record Request(
            String modelo,
            String marca,
            Integer ano,
            @NotNull String placa,
            String cor) {
    }

    public record Update(
            @NotNull Long id,
            @NotNull Pessoa.Id pessoa,
            String modelo,
            String marca,
            Integer ano,
            @NotNull String placa,
            String cor) {
    }
    public record Vincular(
            @NotNull Veiculo.Id veiculo,
            @NotNull Pessoa.Id pessoa,
            Boolean ativo
    ) {}

    public static final RowMapper<Veiculo> ROW_MAPPER =
            (rs, rowNum) ->
                    new Veiculo(
                            rs.getLong("id"),
                            rs.getString("modelo"),
                            rs.getString("marca"),
                            rs.getObject("ano", Integer.class),
                            rs.getString("placa"),
                            rs.getString("cor"));
}
