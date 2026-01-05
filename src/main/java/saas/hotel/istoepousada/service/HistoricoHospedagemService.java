package saas.hotel.istoepousada.service;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.HistoricoHospedagem;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.repository.HistoricoHospedagemRepository;

@Service
public class HistoricoHospedagemService {

    private final HistoricoHospedagemRepository historicoHospedagemRepository;

    public HistoricoHospedagemService(HistoricoHospedagemRepository historicoHospedagemRepository) {
        this.historicoHospedagemRepository = historicoHospedagemRepository;
    }

    /**
     * Ajuste das regras de data:
     * - dataInicio informado e dataFim nulo: busca de dataInicio em diante (>= dataInicio)
     * - dataFim informado e dataInicio nulo: busca de dataFim pra trás (<= dataFim)
     * - ambos informados: mantém busca por range com overlap (pernoite contém/intersecta as datas)
     * - ambos nulos: retorna último histórico
     */
    @Transactional(readOnly = true)
    public HistoricoHospedagem buscar(Long pessoaId, LocalDate dataInicio, LocalDate dataFim) {
        if (pessoaId == null) {
            throw new IllegalArgumentException("pessoaId é obrigatório.");
        }

        // Aqui mantemos a assinatura do repositório, mas ajustamos a interpretação:
        // - Só dataInicio => (dataInicio, dataFim=null) significa "de dataInicio em diante"
        // - Só dataFim    => (dataInicio=null, dataFim) significa "de dataFim pra trás"
        // - Ambos         => range normal
        // - Nenhum        => último histórico
        Optional<HistoricoHospedagem> opt =
                historicoHospedagemRepository.buscarHistorico(pessoaId, dataInicio, dataFim);

        return opt.orElseThrow(
                () -> new NotFoundException("Nenhum histórico de hospedagem encontrado para a pessoa informada."));
    }
}
