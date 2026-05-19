// package saas.hotel.istoepousada.controller;
//
// import jakarta.validation.Valid;
// import java.util.List;
// import java.util.UUID;
// import org.springframework.http.HttpStatus;
// import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.PutMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.ResponseStatus;
// import org.springframework.web.bind.annotation.RestController;
// import saas.hotel.istoepousada.dto.Item;
// import saas.hotel.istoepousada.dto.Pagamento;
// import saas.hotel.istoepousada.dto.Pernoite;
// import saas.hotel.istoepousada.service.PernoiteService;
//
// @RestController
// public class PernoiteController {
//
//  private final PernoiteService service;
//
//  public PernoiteController(PernoiteService service) {
//    this.service = service;
//  }
//
//  // ── Pernoite ────────────────────────────────────────────────────────────────
//
////  @PostMapping("/pernoites")
////  @ResponseStatus(HttpStatus.CREATED)
////  public Pernoite criar(@RequestBody @Valid Pernoite.CreateRequest request) {
////    return service.create(request);
////  }
////
////  @PutMapping("/pernoites/{id}")
////  public Pernoite editar(@PathVariable Long id, @RequestBody @Valid Pernoite.EditRequest
// request) {
////    return service.edit(id, request);
////  }
////
////  @PutMapping("/pernoites/{id}/status")
////  public Pernoite alterarStatus(@PathVariable Long id, @RequestParam Pernoite.Status status) {
////    return service.updateStatus(id, status);
////  }
//
//  // ── Diária – quarto ─────────────────────────────────────────────────────────
//
//  @PutMapping("/diarias/{id}/quarto")
//  public Pernoite.Diaria alterarQuartoDiaria(
//      @PathVariable Long id, @RequestBody @Valid Pernoite.AlterarQuartoRequest request) {
//    return service.updateDiariaQuarto(id, request.quarto_id());
//  }
//
//  // ── Diária – pessoas ────────────────────────────────────────────────────────
//
//  @PostMapping("/diarias/{id}/pessoas")
//  @ResponseStatus(HttpStatus.CREATED)
//  public Pernoite.Diaria adicionarPessoasDiaria(
//      @PathVariable Long id, @RequestBody @Valid Pernoite.AdicionarPessoasRequest request) {
//    return service.addPessoasToDiaria(id, request.pessoa_ids());
//  }
//
//  @DeleteMapping("/diarias/{id}/pessoas/{pessoaId}")
//  public Pernoite.Diaria removerPessoaDiaria(@PathVariable Long id, @PathVariable Long pessoaId) {
//    return service.removePessoaFromDiaria(id, pessoaId);
//  }
//
//  // ── Pernoite – pagamentos ───────────────────────────────────────────────────
//
//  @PostMapping("/pernoites/{id}/pagamentos")
//  @ResponseStatus(HttpStatus.CREATED)
//  public Pernoite adicionarPagamentosPernoite(
//      @PathVariable Long id, @RequestBody List<Pagamento.Request> pagamentos) {
//    return service.addPagamentosToPernoite(id, pagamentos);
//  }
//
//  @DeleteMapping("/pernoites/pagamentos/{uuid}")
//  public Pernoite cancelarPagamentoPernoite(
//      @PathVariable UUID uuid,
//      @RequestBody @Valid Pernoite.CancelamentoPagamentoRequest request) {
//    return service.cancelPernoitePagamento(uuid, request.motivo_cancelamento());
//  }
//
//  // ── Diária – consumos ───────────────────────────────────────────────────────
//
//  @PostMapping("/diarias/{id}/consumos")
//  @ResponseStatus(HttpStatus.CREATED)
//  public Pernoite.Diaria adicionarConsumosDiaria(
//      @PathVariable Long id, @RequestBody List<Item.Consumo.Request> consumos) {
//    return service.addConsumosToDiaria(id, consumos);
//  }
//
//  @DeleteMapping("/diarias/{id}/consumos/{consumoId}")
//  public Pernoite.Diaria removerConsumoDiaria(@PathVariable Long id, @PathVariable Long consumoId)
// {
//    return service.removeConsumoFromDiaria(id, consumoId);
//  }
//
//  // ── Diária – status ─────────────────────────────────────────────────────────
//
//  @PutMapping("/diarias/{id}/status")
//  public Pernoite.Diaria alterarStatusDiaria(
//      @PathVariable Long id, @RequestParam Pernoite.Diaria.Status status) {
//    return service.updateDiariaStatus(id, status);
//  }
// }
