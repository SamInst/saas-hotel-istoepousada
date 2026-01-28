package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.Getter;

public record Quarto(
    Long id,
    String descricao,
    Integer quantidade_pessoas,
    StatusQuarto status_quarto,
    Integer qtd_cama_casal,
    Integer qtd_cama_solteiro,
    Integer qtd_rede,
    Integer qtd_beliche) {
  public static Quarto mapQuarto(ResultSet rs) throws SQLException {
    return mapQuarto(rs, "quarto_");
  }

  public static Quarto mapQuarto(ResultSet rs, String prefix) throws SQLException {
    Long id = rs.getObject(prefix + "id", Long.class);
    String descricao = rs.getString(prefix + "descricao");
    Integer qtdPessoas = rs.getObject(prefix + "qtd_pessoas", Integer.class);

    String statusDb = rs.getString(prefix + "status");
    StatusQuarto status =
        (statusDb == null || statusDb.isBlank()) ? null : StatusQuarto.valueOf(statusDb);

    Integer camaCasal = rs.getObject(prefix + "qtd_cama_casal", Integer.class);
    Integer camaSolteiro = rs.getObject(prefix + "qtd_cama_solteiro", Integer.class);
    Integer rede = rs.getObject(prefix + "qtd_rede", Integer.class);
    Integer beliche = rs.getObject(prefix + "qtd_beliche", Integer.class);

    return new Quarto(id, descricao, qtdPessoas, status, camaCasal, camaSolteiro, rede, beliche);
  }

  @Getter
  public enum StatusQuarto {
    OCUPADO,
    DISPONIVEL,
    RESERVADO,
    LIMPEZA,
    DIARIA_ENCERRADA,
    MANUTENCAO
  }
}
