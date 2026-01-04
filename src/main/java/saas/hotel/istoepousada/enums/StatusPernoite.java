package saas.hotel.istoepousada.enums;

import lombok.Getter;

@Getter
public enum StatusPernoite {
  ATIVO(0),
  DIARIA_ENCERRADA(1),
  FINALIZADO(2),
  CANCELADO(3),
  FINALIZADO_PAGAMENTO_PENDENTE(4);

  private final int value;

  StatusPernoite(int value) {
    this.value = value;
  }
}
