package saas.hotel.istoepousada.dto;

import java.time.LocalDate;
import java.util.List;

public record HistoricoHospedagem(
        Long pessoaId,
        LocalDate dataEntrada,
        LocalDate dataSaida,
        Float total,
        List<DiariaHistorico> diarias
) {

    public record DiariaHistorico(
            Integer numeroDiaria,
            LocalDate dataInicio,
            LocalDate dataFim,
            Integer quantidadePessoas,
            String observacao,
            Float subtotal,
            List<PessoaResumo> pessoas,
            List<SuiteResumo> suites,
            List<PagamentoResumo> pagamentos,
            List<ConsumoResumo> consumos
    ) {}

    public record PessoaResumo(Long id, String nome, Boolean representante) {}

    public record SuiteResumo(Long pernoiteId, Quarto quarto, Float valor, PessoaResumo responsavel) {}

    public record PagamentoResumo(Long id, Float valor, java.time.LocalDateTime dataHora, TipoPagamento tipoPagamento) {}

    public record ConsumoResumo(
            Long id,
            java.time.LocalDateTime dataHora,
            Item item,
            Integer quantidade,
            Float valorTotal,
            TipoPagamento tipoPagamento
    ) {}
}
