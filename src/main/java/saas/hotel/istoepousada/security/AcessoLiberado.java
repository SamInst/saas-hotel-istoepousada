package saas.hotel.istoepousada.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Permite que múltiplas telas acessem um endpoint específico. Quando presente em um método,
 * sobrescreve o @RequireTela definido na classe. O usuário precisa ter acesso a pelo menos UMA das
 * telas listadas.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AcessoLiberado {
  String[] value();
}
