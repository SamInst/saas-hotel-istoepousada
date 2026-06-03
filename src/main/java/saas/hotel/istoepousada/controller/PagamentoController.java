package saas.hotel.istoepousada.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.service.HospedagemService;
import saas.hotel.istoepousada.service.PagamentoService;

@RestController
@RequestMapping("/pagamento")
public class PagamentoController {
  private final PagamentoService service;
  private final HospedagemService hospedagemService;

  public PagamentoController(PagamentoService service, HospedagemService hospedagemService) {
    this.service = service;
    this.hospedagemService = hospedagemService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Pagamento create(@RequestBody Pagamento.Request req) {
    return service.criar(req);
  }

  /** Cria um único pagamento e vincula a todas as reservas de um grupo. */
  @PostMapping("/grupo/{grupoId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void adicionarPagamentoGrupo(
      @PathVariable Long grupoId, @RequestBody Pagamento.Request req) {
    hospedagemService.adicionarPagamentoGrupo(grupoId, req);
  }

  @GetMapping
  public List<Pagamento> findAll() {
    return service.listar();
  }

  @PutMapping
  public Pagamento update(@RequestBody Pagamento.Update pagamento) {
    return service.atualizar(pagamento);
  }

  @PutMapping("/{id}/cancelar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelarPagamento(
      @PathVariable UUID id, @RequestBody Pagamento.CancelamentoRequest request) {
    service.cancelarPagamento(id, request.motivo_cancelamento());
  }
}
