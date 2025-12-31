package saas.hotel.istoepousada.dto;

public record Veiculo(Long id, String modelo, String marca, Integer ano, String placa, String cor) {
  public Veiculo withId(Long id) {
    return new Veiculo(id, modelo, marca, ano, placa, cor);
  }

  public static Veiculo mapVeiculo(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new Veiculo(
        rs.getLong("id"),
        rs.getString("modelo"),
        rs.getString("marca"),
        rs.getObject("ano", Integer.class),
        rs.getString("placa"),
        rs.getString("cor"));
  }
}
