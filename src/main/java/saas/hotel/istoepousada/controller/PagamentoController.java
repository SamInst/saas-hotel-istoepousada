package saas.hotel.istoepousada.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.service.PagamentoService;

@RestController
@RequestMapping("/pagamento")
public class PagamentoController {
  private final PagamentoService service;

  public PagamentoController(PagamentoService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Pagamento create(@RequestBody Pagamento.Request req) {
    return service.criar(req);
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
