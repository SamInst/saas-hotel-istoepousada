package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/** Registro de uma alteração feita em um relatório (quem alterou e o que mudou). */
public record RelatorioHistorico(
    @NotNull Long id,
    @NotNull String acao,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss") LocalDateTime data_hora,
    Funcionario.Nome funcionario,
    @NotNull List<Alteracao> alteracoes) {

  /** Um campo alterado: valor anterior (de) e novo valor (para). */
  public record Alteracao(@NotNull String campo, String de, String para) {}
}
