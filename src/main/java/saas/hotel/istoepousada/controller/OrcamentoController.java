package saas.hotel.istoepousada.controller;

import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Hospedagem;
import saas.hotel.istoepousada.dto.MotivoCancelamentoHospedagem;
import saas.hotel.istoepousada.dto.Orcamento;
import saas.hotel.istoepousada.service.HospedagemService;

import java.util.List;


@RestController
@RequestMapping("/orcamento")
public class OrcamentoController {

  private final HospedagemService hospedagemService;

  public OrcamentoController(HospedagemService hospedagemService) {
    this.hospedagemService = hospedagemService;
  }

  @PostMapping
  public void criar(@RequestBody List<Hospedagem.Request> requests) {
    requests.forEach(hospedagemService::criarOrcamento);
  }

  @PutMapping("/{hospedagemId}/cancelar")
  public void cancelar(
      @PathVariable Long hospedagemId,
      @RequestBody MotivoCancelamentoHospedagem.Request motivo) {
    hospedagemService.cancelarOrcamento(hospedagemId, motivo);
  }

  @PutMapping("/editar")
  public void editar(@RequestBody Hospedagem.Request request) {
    hospedagemService.editarOrcamento(request);
  }

  @PostMapping("/{orcamentoId}/pessoas")
  public void adicionarPessoas(
      @PathVariable Long orcamentoId,
      @RequestBody List<Hospedagem.PessoaHospedagemOrcamento> pessoas) {
    hospedagemService.adicionarPessoasHospedagemOrcamento(orcamentoId, pessoas);
  }

  @DeleteMapping("/{orcamentoId}/pessoas")
  public void removerPessoas(
      @PathVariable Long orcamentoId, @RequestBody List<Long> pessoasIds) {
    hospedagemService.removerPessoasOrcamento(orcamentoId, pessoasIds);
  }

  @GetMapping("/{hospedagemId}")
  public Orcamento buscar(@PathVariable Long hospedagemId) {
    return hospedagemService.buscarOrcamento(hospedagemId);
  }
}
