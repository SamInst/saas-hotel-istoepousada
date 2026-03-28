package saas.hotel.istoepousada.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.CategoriaItem;
import saas.hotel.istoepousada.dto.Item;
import saas.hotel.istoepousada.service.ItemService;

import java.util.List;

@RestController
@RequestMapping
public class ItemController {

  private final ItemService itemService;

  public ItemController(ItemService itemService) {
    this.itemService = itemService;
  }

  @GetMapping("/item/buscar")
  public Page<Item> buscar(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String termo,
      @RequestParam(required = false) Long categoria_id,
      Pageable pageable) {
    return itemService.buscar(
        id, termo, categoria_id, pageable);
  }

  @PostMapping("/item")
  @ResponseStatus(HttpStatus.CREATED)
  public Item criarItem(@RequestBody Item.Request request) {
    return itemService.criarItem(request);
  }

  @PutMapping("/item")
  public Item atualizarItem(@RequestBody Item.Update request) {
    return itemService.atualizarItem(request);
  }

  @GetMapping("/item/{id}/historico-reposicao")
  public List<Item.HistoricoEstoque> listarHistoricoReposicao(@PathVariable Long id) {
    return itemService.listarHistoricoReposicaoPorItemId(id);
  }

  @PostMapping("/item/historico-reposicao")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void registrarHistoricoReposicao(@RequestBody Item.HistoricoEstoque.Request request) {
    itemService.registrarHistoricoReposicao(request);
  }

  @PutMapping("/item/historico-reposicao")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void atualizarHistoricoReposicao(@RequestBody Item.HistoricoEstoque.Update request) {
    itemService.atualizarHistoricoReposicao(request);
  }

  @GetMapping("/item/estoque")
  public Item.HistoricoEstoque.Estoque listarEstoque() {
    return itemService.listarEstoque();
  }

  @GetMapping("/item/consumo")
  public List<Item.Consumo> listarConsumos(
      @PageableDefault(size = 50) Pageable pageable) {
    return itemService.listarConsumos(pageable);
  }

  @PatchMapping("/item/consumo/{id}/cancelar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelarConsumo(@PathVariable Long id) {
    itemService.cancelarConsumo(id);
  }

  @PostMapping("/item/consumo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void consumirItem(@RequestBody Item.Consumo.Request request) {
    itemService.consumirItem(request);
  }

  @PostMapping("/categoria")
  @ResponseStatus(HttpStatus.CREATED)
  public Long criarCategoria(@RequestBody CategoriaItem.Request request) {
    return itemService.criarCategoria(request);
  }

  @PutMapping("/categoria/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void atualizarCategoria(@RequestBody CategoriaItem.Update request) {
    itemService.atualizarCategoria(request);
  }

  @GetMapping("/categoria/{id}")
  public CategoriaItem buscarCategoriaPorId(@PathVariable Long id) {
    return itemService.buscarCategoriaPorId(id);
  }

  @GetMapping("/categorias")
  public List<CategoriaItem> listarCategorias() {
    return itemService.listarCategorias();
  }
}
