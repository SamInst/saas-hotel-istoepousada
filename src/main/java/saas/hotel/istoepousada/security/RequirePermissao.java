package saas.hotel.istoepousada.security;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermissao {
  /** Ex.: {"RELATORIO_EXTRATO_EXPORTAR", "RELATORIO_EXTRATO_EXCLUIR"} */
  String[] value();
}
