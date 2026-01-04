package saas.hotel.istoepousada.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.hotel.istoepousada.dto.HistoricoHospedagem;
import saas.hotel.istoepousada.service.HistoricoHospedagemService;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/historico")
public class HistoricoHospedagemController {
    private final HistoricoHospedagemService historicoHospedagemService;

    public HistoricoHospedagemController(HistoricoHospedagemService historicoHospedagemService) {
        this.historicoHospedagemService = historicoHospedagemService;
    }

    @GetMapping
    public Optional<HistoricoHospedagem> buscarHistorico(Long pessoaId, LocalDate dataInicio, LocalDate dataFim) {
        return historicoHospedagemService.buscarHistorico(pessoaId, dataInicio, dataFim);
    }
}
