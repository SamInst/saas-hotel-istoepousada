package saas.hotel.istoepousada.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.dto.Recepcao;
import saas.hotel.istoepousada.security.AcessoLiberado;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.QuartoService;

@RestController
@RequestMapping("/quarto")
@RequireTela("APARTAMENTOS")
public class QuartoController {

  private final QuartoService quartoService;

  public QuartoController(QuartoService quartoService) {
    this.quartoService = quartoService;
  }

  // ── Quarto CRUD ──────────────────────────────────────────────────────────────

  @GetMapping
  @AcessoLiberado({"FINANCEIRO", "ITENS"})
  public Page<Quarto> listar(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String termo,
      @RequestParam(required = false) Quarto.Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "900") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return quartoService.buscar(id, termo, status, pageable);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Quarto criar(@RequestBody Quarto.Request quarto) {
    return quartoService.criar(quarto);
  }

  @PutMapping
  public Quarto atualizar(@RequestBody Quarto.Update quarto) {
    return quartoService.atualizar(quarto);
  }

  @PatchMapping("/{id}/status")
  public Quarto alterarStatus(@PathVariable Long id, @RequestParam Quarto.Status status) {
    return quartoService.alterarStatus(id, status);
  }

  // ── Recepção ─────────────────────────────────────────────────────────────────

  @GetMapping("/recepcao")
  @AcessoLiberado({"FINANCEIRO", "ITENS"})
  public Recepcao.QuartoData recepcao(
      @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data) {
    return quartoService.buscarRecepcao(data);
  }

  // ── Itens ─────────────────────────────────────────────────────────────────────

  @GetMapping("/{id}/itens")
  public List<Quarto.ItemQuarto> listarItens(@PathVariable Long id) {
    return quartoService.listarItens(id);
  }

  @PostMapping("/{quartoId}/itens")
  @ResponseStatus(HttpStatus.CREATED)
  public Quarto.ItemQuarto adicionarItem(
      @PathVariable Long quartoId, @RequestBody Quarto.QuartoItem.Request req) {
    return quartoService.adicionarItem(quartoId, req);
  }

  @PutMapping("/itens")
  public Quarto.ItemQuarto atualizarItem(@RequestBody Quarto.QuartoItem.Update req) {
    return quartoService.atualizarItem(req);
  }

  @PatchMapping("/itens/consumir")
  public Quarto.ItemQuarto consumirItem(@RequestBody Quarto.QuartoItem.Consumir req) {
    return quartoService.consumirItem(req);
  }

  @PatchMapping("/itens/repor")
  public Quarto.ItemQuarto reporItem(@RequestBody Quarto.QuartoItem.Repor req) {
    return quartoService.reporItem(req);
  }

  // ── Manutenção ────────────────────────────────────────────────────────────────

  @PostMapping("/manutencao")
  @ResponseStatus(HttpStatus.CREATED)
  public Quarto.QuartoManutencao criarManutencao(@RequestBody Quarto.QuartoManutencao.Request req) {
    return quartoService.criarManutencao(req);
  }

  @PutMapping("/manutencao/{id}")
  public Quarto.QuartoManutencao atualizarManutencao(
      @PathVariable Long id, @RequestBody Quarto.QuartoManutencao.Update req) {
    return quartoService.atualizarManutencao(req);
  }

  @PatchMapping("/manutencao/{id}/finalizar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void finalizarManutencao(@PathVariable Long id) {
    quartoService.finalizarManutencao(id);
  }

  // ── Limpeza ───────────────────────────────────────────────────────────────────

  @PostMapping("/{quartoId}/limpeza")
  @ResponseStatus(HttpStatus.CREATED)
  public Quarto.QuartoLimpeza acionarLimpeza(
      @PathVariable Long quartoId, @RequestBody Quarto.QuartoLimpeza.Request req) {
    return quartoService.acionarLimpeza(quartoId, req);
  }

  @PatchMapping("/limpeza/{id}/finalizar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void finalizarLimpeza(@PathVariable Long id) {
    quartoService.finalizarLimpeza(id);
  }
}
