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

  @Transactional(readOnly = true)
  public HistoricoHospedagem buscar(Long pessoaId, LocalDate dataInicio, LocalDate dataFim) {
    if (pessoaId == null) {
      throw new IllegalArgumentException("pessoaId é obrigatório.");
    }

    Optional<HistoricoHospedagem> opt =
        historicoHospedagemRepository.buscarHistorico(pessoaId, dataInicio, dataFim);

    return opt.orElseThrow(
        () ->
            new NotFoundException(
                "Nenhum histórico de hospedagem encontrado para a pessoa informada."));
  }
}
