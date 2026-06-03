package saas.hotel.istoepousada.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Categoria;
import saas.hotel.istoepousada.security.AcessoLiberado;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.CategoriaService;

@RestController
@RequestMapping("/quarto-categoria")
@RequireTela("CATEGORIAS")
public class CategoriaController {

  private final CategoriaService categoriaService;

  public CategoriaController(CategoriaService categoriaService) {
    this.categoriaService = categoriaService;
  }

  @GetMapping
  @AcessoLiberado({"FINANCEIRO", "APARTAMENTOS"})
  public Page<Categoria> listar(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String termo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "900") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return categoriaService.buscar(id, termo, pageable);
  }

  @GetMapping("/{id}")
  @AcessoLiberado({"FINANCEIRO", "APARTAMENTOS"})
  public Categoria buscarPorId(@PathVariable Long id) {
    return categoriaService.buscarPorId(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Categoria criar(@RequestBody Categoria.Request request) {
    return categoriaService.criar(request);
  }

  @PutMapping
  public Categoria atualizar(@RequestBody Categoria.Update request) {
    return categoriaService.atualizar(request);
  }

  //  @GetMapping("/calcular")
  //  public float calcularValorTotal(
  //          @RequestParam Long categoriaId,
  //          @RequestParam LocalDate dataEntrada,
  //          @RequestParam int diarias,
  //          @RequestParam int quantidadePessoas) {
  //    return categoriaService.calcularValorTotal(categoriaId, dataEntrada, diarias,
  // quantidadePessoas);
  //  }
}
