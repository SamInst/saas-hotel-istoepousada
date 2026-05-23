package saas.hotel.istoepousada.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Hospedagem;
import saas.hotel.istoepousada.dto.MotivoCancelamentoHospedagem;
import saas.hotel.istoepousada.dto.Orcamento;
import saas.hotel.istoepousada.service.HospedagemService;

@RestController
@RequestMapping("/orcamento")
public class OrcamentoController {

  private final HospedagemService hospedagemService;

  public OrcamentoController(HospedagemService hospedagemService) {
    this.hospedagemService = hospedagemService;
  }

  @PostMapping
  public void criar(@RequestBody Orcamento.Request request) {
    hospedagemService.criarOrcamento(request);
  }

  @PutMapping("/{orcamentoId}/cancelar")
  public void cancelar(
      @PathVariable Long orcamentoId, @RequestBody MotivoCancelamentoHospedagem.Request motivo) {
    hospedagemService.cancelarOrcamento(orcamentoId, motivo);
  }

  @PutMapping("/editar")
  public void editar(@RequestBody Orcamento.Request request) {
    hospedagemService.editarOrcamento(request);
  }

  @PostMapping("/{hospedagemId}/pessoas")
  public void adicionarPessoas(
      @PathVariable Long hospedagemId,
      @RequestBody List<Hospedagem.PessoaHospedagemOrcamento.Request> pessoas) {
    hospedagemService.adicionarPessoasHospedagemOrcamento(hospedagemId, pessoas);
  }

  @DeleteMapping("/pessoas")
  public void removerPessoas(@RequestBody List<Long> pessoasOrcamentoIds) {
    hospedagemService.removerPessoasOrcamento(pessoasOrcamentoIds);
  }

  @GetMapping("/buscar")
  public List<Orcamento> buscar(
      @RequestParam(required = false) Long orcamentoId,
      @RequestParam(required = false) String nomeSolicitante) {
    return hospedagemService.buscarOrcamento(orcamentoId, nomeSolicitante);
  }
}
