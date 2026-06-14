package saas.hotel.istoepousada.dto;

import java.util.List;
import org.springframework.jdbc.core.RowMapper;

/**
 * Ajuste manual de preço aplicado a uma hospedagem ("Gerenciar Preços").
 *
 * <p>Modos mutuamente exclusivos (apenas um por vez):
 *
 * <ul>
 *   <li>{@code valor_diaria} – sobrescreve o valor das diárias existentes;
 *   <li>{@code valor_desconto} – ajuste absoluto sobre o total;
 *   <li>{@code porcentagem} – ajuste percentual sobre o total.
 * </ul>
 *
 * O sinal (desconto/adicional) e todo o cálculo são feitos no front-end; o back-end apenas
 * persiste o snapshot, o {@code valor_total} resultante e (no modo diária) os valores das diárias.
 */
public record HospedagemNovoPreco(
    Long id,
    Integer quantidade_diarias,
    Integer quantidade_pessoas,
    Double valor_diaria,
    Integer porcentagem,
    Double valor_desconto,
    Funcionario.Nome funcionario) {

  public record Request(
      Integer quantidade_diarias,
      Integer quantidade_pessoas,
      Double valor_diaria,
      Integer porcentagem,
      Double valor_desconto,
      /* total já ajustado, calculado no front-end */
      Double valor_total,
      /* presente apenas no modo "valor por diária" — novos valores das diárias existentes */
      List<DiariaValor> diarias) {}

  public record DiariaValor(Long id, Double valor) {}

  public static final RowMapper<HospedagemNovoPreco> MAPPER =
      (rs, rowNum) -> {
        Long funcionarioId = rs.getObject("novo_preco_funcionario_id", Long.class);
        return new HospedagemNovoPreco(
            rs.getLong("novo_preco_id"),
            rs.getObject("novo_preco_quantidade_diarias", Integer.class),
            rs.getObject("novo_preco_quantidade_pessoas", Integer.class),
            rs.getObject("novo_preco_valor_diaria", Double.class),
            rs.getObject("novo_preco_porcentagem", Integer.class),
            rs.getObject("novo_preco_valor_desconto", Double.class),
            funcionarioId == null
                ? null
                : new Funcionario.Nome(funcionarioId, rs.getString("novo_preco_funcionario_nome")));
      };
}
