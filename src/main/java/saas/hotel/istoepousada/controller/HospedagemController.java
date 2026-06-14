package saas.hotel.istoepousada.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Hospedagem;
import saas.hotel.istoepousada.dto.HospedagemNovoPreco;
import saas.hotel.istoepousada.dto.Item;
import saas.hotel.istoepousada.dto.MotivoCancelamentoHospedagem;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.dto.PageResult;
import saas.hotel.istoepousada.service.HospedagemService;

@RestController
@RequestMapping("/hospedagem")
public class HospedagemController {

  private final HospedagemService hospedagemService;

  public HospedagemController(HospedagemService hospedagemService) {
    this.hospedagemService = hospedagemService;
  }

  // ── Consultas ────────────────────────────────────────────────────────────────

  @GetMapping({"/buscar", ""})
  public List<Hospedagem> buscar(
      @RequestParam(required = false) List<Hospedagem.Status> status,
      @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data,
      @RequestParam(required = false) Integer mes,
      @RequestParam(required = false) Integer ano,
      @RequestParam(required = false) String nomeTitular) {
    return hospedagemService.buscar(status, data, mes, ano, nomeTitular);
  }

  @GetMapping("/{hospedagemId}")
  public Hospedagem buscarPorId(@PathVariable Long hospedagemId) {
    return hospedagemService.buscarPorId(hospedagemId);
  }

  /** Reservas de um quarto, paginadas. periodo: "anteriores" | "proximas" | (vazio = mês/ano). */
  @GetMapping("/quarto/{quartoId}")
  public PageResult<Hospedagem> buscarPorQuarto(
      @PathVariable Long quartoId,
      @RequestParam(required = false) Integer mes,
      @RequestParam(required = false) Integer ano,
      @RequestParam(required = false) String periodo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "8") int size) {
    return hospedagemService.buscarPorQuarto(quartoId, mes, ano, periodo, page, size);
  }

  @PutMapping
  public Hospedagem editar(@RequestBody Hospedagem.Request request) {
    return hospedagemService.editarHospedagem(request);
  }

  // ── Ciclo de vida ────────────────────────────────────────────────────────────

  @PutMapping("/{hospedagemId}/ativar")
  public void ativar(@PathVariable Long hospedagemId, @RequestBody Hospedagem.Request request) {
    hospedagemService.ativarPernoite(hospedagemId, request);
  }

  @PutMapping("/{hospedagemId}/cancelar")
  public void cancelar(
      @PathVariable Long hospedagemId, @RequestBody MotivoCancelamentoHospedagem.Request motivo) {
    hospedagemService.cancelarPernoite(hospedagemId, motivo);
  }

  @PutMapping("/{hospedagemId}/finalizar")
  public void finalizar(@PathVariable Long hospedagemId) {
    hospedagemService.finalizarPernoite(hospedagemId);
  }

  /** Altera apenas o status da hospedagem (validando a transição). */
  @PutMapping("/{hospedagemId}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void alterarStatus(
      @PathVariable Long hospedagemId, @RequestParam Hospedagem.Status status) {
    hospedagemService.alterarStatusComValidacao(hospedagemId, status);
  }

  @PutMapping("/{hospedagemId}/finalizar-pendente")
  public void finalizarPendente(@PathVariable Long hospedagemId) {
    hospedagemService.finalizarPernoitePagamentoPendente(hospedagemId);
  }

  // ── Diárias ──────────────────────────────────────────────────────────────────

  @PostMapping("/{hospedagemId}/diarias")
  public void adicionarDiarias(
      @PathVariable Long hospedagemId, @RequestBody List<Hospedagem.Diaria.Request> diarias) {
    hospedagemService.adicionarDiarias(hospedagemId, diarias);
  }

  // ── Pagamentos ───────────────────────────────────────────────────────────────

  @PostMapping("/{hospedagemId}/pagamentos")
  public void adicionarPagamentos(
      @PathVariable Long hospedagemId,
      @RequestParam(required = false) Long quartoId,
      @RequestBody Hospedagem.Request request,
      @RequestParam(required = false) Hospedagem.Status status) {
    hospedagemService.adicionarPagamentos(hospedagemId, quartoId, request.pagamentos(), status);
  }

  record PagamentoMultiploRequest(List<Long> hospedagem_ids, Pagamento.Request pagamento) {}

  @PostMapping("/pagamento-multiplo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void adicionarPagamentoMultiplo(@RequestBody PagamentoMultiploRequest request) {
    hospedagemService.adicionarPagamentoMultiplasHospedagens(
        request.hospedagem_ids(), request.pagamento());
  }

  // ── Gerenciar Preços (ajuste manual) ──────────────────────────────────────────

  @PostMapping("/{hospedagemId}/preco")
  public Hospedagem gerenciarPreco(
      @PathVariable Long hospedagemId, @RequestBody HospedagemNovoPreco.Request request) {
    return hospedagemService.gerenciarPreco(hospedagemId, request);
  }

  // ── Consumo ──────────────────────────────────────────────────────────────────

  @PostMapping("/{hospedagemId}/consumo")
  public void adicionarConsumo(
      @PathVariable Long hospedagemId, @RequestBody Item.Consumo.Request request) {
    hospedagemService.adicionarConsumo(hospedagemId, request);
  }

  @PutMapping("/consumo")
  public void editarConsumo(@RequestBody Item.Consumo.Request request) {
    hospedagemService.editarConsumo(request);
  }

  // ── Pessoas ──────────────────────────────────────────────────────────────────

  @PostMapping("/{hospedagemId}/pessoas")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void adicionarPessoas(
      @PathVariable Long hospedagemId, @RequestBody List<Long> pessoasIds) {
    hospedagemService.adicionarPessoas(hospedagemId, pessoasIds);
  }

  @DeleteMapping("/{hospedagemId}/pessoas")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removerPessoas(@PathVariable Long hospedagemId, @RequestBody List<Long> pessoasIds) {
    hospedagemService.removerPessoas(hospedagemId, pessoasIds);
  }

  // ── Cancelamento ─────────────────────────────────────────────────────────────
  @PutMapping("/motivo-cancelamento")
  public void editarMotivoCancelamento(@RequestBody MotivoCancelamentoHospedagem.Request request) {
    hospedagemService.editarMotivoCancelamento(request);
  }
}
