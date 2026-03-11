package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.repository.PessoaRepository;
import saas.hotel.istoepousada.repository.RelatorioRepository;

import java.time.LocalDate;

@Service
public class RelatorioService {

    private final RelatorioRepository relatorioRepository;
    private final PessoaRepository pessoaRepository;

    public RelatorioService(
            RelatorioRepository relatorioRepository,
            PessoaRepository pessoaRepository) {
        this.relatorioRepository = relatorioRepository;
        this.pessoaRepository = pessoaRepository;
    }

    public Relatorio.Extrato buscar(
            Long id,
            LocalDate dataInicio,
            LocalDate dataFim,
            Long funcionarioId,
            Long quartoId,
            Long tipoPagamentoId,
            Relatorio.Registro registro,
            Boolean despesaPessoal,
            Pageable pageable) {
        if (dataInicio == null && dataFim == null) {
            LocalDate hoje = LocalDate.now();
            dataInicio = hoje.minusDays(1);
            dataFim = hoje;
        }
        return relatorioRepository.buscar(
                id,
                dataInicio,
                dataFim,
                funcionarioId,
                quartoId,
                tipoPagamentoId,
                registro,
                despesaPessoal,
                pageable);
    }

    @Transactional
    public Relatorio criar(Relatorio.Request request) {
        validarRequest(request);
        return relatorioRepository.insert(request);
    }

    @Transactional
    public Relatorio atualizar(Relatorio.Update relatorio) {
        if (relatorio.id() == null) throw new IllegalArgumentException("id é obrigatório.");
        return relatorioRepository.update(relatorio);
    }

    private void validarRequest(Relatorio.Request request) {
        if (request == null) throw new IllegalArgumentException("Request é obrigatória.");
        if (!StringUtils.hasText(request.relatorio()))
            throw new IllegalArgumentException("Descrição do relatório é obrigatória.");
        if (request.valor() == null) throw new IllegalArgumentException("valor é obrigatório.");
        if (request.t() == null)
            throw new IllegalArgumentException("tipoPagamentoId é obrigatório.");
    }
}
