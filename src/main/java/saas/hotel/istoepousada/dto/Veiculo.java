package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.RowMapper;

public record Veiculo(
    @NotNull Long id,
    String modelo,
    String marca,
    Integer ano,
    @NotNull String placa,
    String cor,
    Tipo tipo,
    Integer codigo_cor) {
  public record Id(@NotNull Long id) {}

  public record Request(
      String modelo, String marca, Integer ano, @NotNull String placa,
      String cor,
      Tipo tipo,
      Integer codigo_cor) {}

  public record Update(
      @NotNull Long id,
      String modelo,
      String marca,
      Integer ano,
      @NotNull String placa,
      String cor,
      Tipo tipo,
      Integer codigo_cor) {}

  public record Vincular(@NotNull Veiculo.Id veiculo, @NotNull Pessoa.Id pessoa, Boolean ativo) {}

  public enum Tipo {
    CARRO,
    MOTO,
    PICKUP,
    SUV,
    VAN,
    CAMINHAO,
    ONIBUS,
    MICROONIBUS,
    QUADRICICLO,
    TRATOR;

    public static Tipo map(String tipo) {
      if (tipo == null || tipo.isBlank()) return null;
      try {
        return Tipo.valueOf(tipo.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }
  }

  public static String tipoName(Tipo tipo) {
    return tipo == null ? null : tipo.name();
  }

  public static final RowMapper<Veiculo> ROW_MAPPER =
      (rs, rowNum) -> {
        Long veiculoId = rs.getObject("veiculo_id", Long.class);
        if (veiculoId == null) return null;

        return new Veiculo(
            veiculoId,
            rs.getString("veiculo_modelo"),
            rs.getString("veiculo_marca"),
            rs.getObject("veiculo_ano", Integer.class),
            rs.getString("veiculo_placa"),
            rs.getString("veiculo_cor"),
            Tipo.map(rs.getString("veiculo_tipo")),
            rs.getObject("veiculo_codigo_cor", Integer.class));
      };
}
