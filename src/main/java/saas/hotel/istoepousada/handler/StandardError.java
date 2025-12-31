package saas.hotel.istoepousada.handler;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public record StandardError(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm", timezone = "GMT")
        Instant dataHora,
    Integer status,
    String erro,
    String mensagem,
    String caminho) {

  public StandardError(
      Instant dataHora, Integer status, String erro, String mensagem, String caminho) {
    this.dataHora = dataHora;
    this.status = status;
    this.erro = erro;
    this.mensagem = mensagem;
    this.caminho = caminho;
  }
}
