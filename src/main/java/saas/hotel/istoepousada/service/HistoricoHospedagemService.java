package saas.hotel.istoepousada.service;

import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.dto.HistoricoHospedagem;
import saas.hotel.istoepousada.repository.HistoricoHospedagemRepository;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class HistoricoHospedagemService {

    private final HistoricoHospedagemRepository historicoHospedagemRepository;

    public HistoricoHospedagemService(HistoricoHospedagemRepository historicoHospedagemRepository) {
        this.historicoHospedagemRepository = historicoHospedagemRepository;
    }

    public Optional<HistoricoHospedagem> buscarHistorico(Long pessoaId, LocalDate dataInicio, LocalDate dataFim) {
        return historicoHospedagemRepository.buscarHistorico(pessoaId, dataInicio, dataFim);
    }
}
