package saas.hotel.istoepousada.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Sazonalidade;
import saas.hotel.istoepousada.security.AcessoLiberado;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.SazonabilidadeService;

@RestController
@RequestMapping("/sazonalidade")
@RequireTela("CATEGORIAS")
public class SazonabilidadeController {

  private final SazonabilidadeService sazonabilidadeService;

  public SazonabilidadeController(SazonabilidadeService sazonabilidadeService) {
    this.sazonabilidadeService = sazonabilidadeService;
  }

  @GetMapping
  @AcessoLiberado({"FINANCEIRO", "APARTAMENTOS"})
  public Page<Sazonalidade> listar(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String termo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "900") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return sazonabilidadeService.buscar(id, termo, pageable);
  }

  @GetMapping("/{id}")
  @AcessoLiberado({"FINANCEIRO", "APARTAMENTOS"})
  public Sazonalidade buscarPorId(@PathVariable Long id) {
    return sazonabilidadeService.buscarPorId(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Sazonalidade criar(@RequestBody Sazonalidade.Request request) {
    return sazonabilidadeService.criar(request);
  }

  @PutMapping
  public Sazonalidade atualizar(@RequestBody Sazonalidade.Update request) {
    return sazonabilidadeService.atualizar(request);
  }

  // ── Vínculo com categoria ─────────────────────────────────────────────────

  @PostMapping("/vincular")
  @ResponseStatus(HttpStatus.CREATED)
  public void vincular(@RequestBody Sazonalidade.VinculoCategoriaRequest request) {
    sazonabilidadeService.vincular(request);
  }

  @PutMapping("/vincular")
  public void toggleAtivo(@RequestBody Sazonalidade.VinculoCategoriaToggle request) {
    sazonabilidadeService.toggleAtivo(request);
  }

  @DeleteMapping("/vincular/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removerVinculo(@PathVariable Long id) {
    sazonabilidadeService.removerVinculo(id);
  }
}
