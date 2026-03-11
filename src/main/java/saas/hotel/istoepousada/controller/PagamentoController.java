package saas.hotel.istoepousada.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.service.PagamentoService;

@RestController
@RequestMapping("/api/pagamentos")
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

  @GetMapping("/{id}")
  public Pagamento findById(@PathVariable UUID id) {
    return service.buscar(id);
  }

  @GetMapping
  public List<Pagamento> findAll() {
    return service.listar();
  }

  @PutMapping("/{id}")
  public Pagamento update(@PathVariable UUID id, @RequestBody Pagamento.Request req) {
    return service.atualizar(id, req);
  }

  @DeleteMapping("/cancelar/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelarPagamento(@PathVariable UUID id) {
    service.cancelarPagamento(id);
  }
}
