package saas.hotel.istoepousada.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Quarto;
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

  @GetMapping("/{id}/itens")
  public List<Quarto.ItemQuarto> listarItens(@PathVariable Long id) {
    return quartoService.listarItens(id);
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
}
