package saas.hotel.istoepousada.enums;

import lombok.Getter;

@Getter
public enum StatusQuarto {
  OCUPADO(1),
  DISPONIVEL(2),
  RESERVADO(3),
  LIMPEZA(4),
  DIARIA_ENCERRADA(5),
  MANUTENCAO(6);

  private final int codigo;

  StatusQuarto(int codigo) {
    this.codigo = codigo;
  }

  public static StatusQuarto fromCodigo(int codigo) {
    for (StatusQuarto status : StatusQuarto.values()) {
      if (status.getCodigo() == codigo) {
        return status;
      }
    }
    throw new IllegalArgumentException("Código inválido: " + codigo);
  }
}
