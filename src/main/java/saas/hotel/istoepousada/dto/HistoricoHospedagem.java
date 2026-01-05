package saas.hotel.istoepousada.dto;

import java.util.List;

public record HistoricoHospedagem(
        String tipoHospedagem,
        Integer quantidadeHospedagens,
        Integer totalDiasHospedado,
        Float valorTotal,
        List<DadosPernoite> pernoites
) {

    public record DadosPernoite(
            Pernoite pernoite,
            List<DadosDiaria> dadosDiariaList,
            Float valorTotalHospedagem
    ) {
        public record DadosDiaria(
                Diaria diaria,
                Pessoa representante,
                List<Pessoa> acompanhantes,
                List<DiariaPagamento> pagamentos,
                List<DiariaConsumo> consumos,
                Float subTotal
        ) {}
    }
}
