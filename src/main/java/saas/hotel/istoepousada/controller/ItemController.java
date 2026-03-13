package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.CategoriaItem;
import saas.hotel.istoepousada.dto.Item;
import saas.hotel.istoepousada.service.ItemService;

@Tag(
    name = "Itens/Categorias/Almoxarifado",
    description = "Endpoints de cadastro, edição e consulta de itens, categorias e históricos.")
@RestController
@RequestMapping()
public class ItemController {

  private final ItemService itemService;

  public ItemController(ItemService itemService) {
    this.itemService = itemService;
  }

  @Operation(
      summary = "Buscar itens",
      description =
          "Busca itens com filtros opcionais de id, termo, categoria e período de cadastro.")
  @ApiResponse(responseCode = "200", description = "Itens encontrados")
  @GetMapping("/item/buscar")
  public Page<Item> buscar(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String termo,
      @RequestParam(required = false) Long categoriaId,
      @RequestParam(required = false) LocalDate dataInicioCadastro,
      @RequestParam(required = false) LocalDate dataFimCadastro,
      Pageable pageable) {

    return itemService.buscar(
        id, termo, categoriaId, dataInicioCadastro, dataFimCadastro, pageable);
  }

  @Operation(summary = "Buscar item por id")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Item encontrado"),
    @ApiResponse(responseCode = "404", description = "Item não encontrado")
  })
  @GetMapping("/item/{id}")
  public Item buscarPorId(@PathVariable Long id) {
    return itemService.buscarPorId(id);
  }

  @Operation(summary = "Criar novo item")
  @ApiResponse(responseCode = "201", description = "Item criado")
  @PostMapping("/item")
  @ResponseStatus(HttpStatus.CREATED)
  public Item criarItem(@RequestBody Item.Request request) {
    return itemService.criarItem(request);
  }

  @Operation(summary = "Atualizar item")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Item atualizado"),
    @ApiResponse(responseCode = "404", description = "Item não encontrado")
  })
  @PutMapping("/item")
  public Item atualizarItem(@RequestBody Item.Update request) {
    return itemService.atualizarItem(request);
  }

  @Operation(
      summary = "Listar histórico de preços de um item",
      description = "Retorna o histórico de preço de um item em ordem decrescente de data.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Histórico de preço do item",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array =
                    @ArraySchema(schema = @Schema(implementation = Item.HistoricoPreco.class)))),
    @ApiResponse(responseCode = "404", description = "Item não encontrado")
  })
  @GetMapping("/item/{id}/historico-preco")
  public List<Item.HistoricoPreco> listarHistoricoPreco(
      @Parameter(description = "ID do item", example = "85", required = true) @PathVariable
          Long id) {
    return itemService.listarHistoricoPrecoPorItemId(id);
  }

  @Operation(
      summary = "Registrar histórico de preço",
      description = "Registra um novo histórico de preço para um item.")
  @PostMapping("/item/historico-preco")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void registrarHistoricoPreco(@RequestBody Item.HistoricoPreco.Request request) {
    itemService.registrarHistoricoPreco(request);
  }

  @Operation(
      summary = "Listar histórico de reposições de um item",
      description = "Retorna o histórico de reposições de estoque de um item.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Histórico de reposições do item",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array =
                    @ArraySchema(
                        schema = @Schema(implementation = Item.HistoricoReposicao.class)))),
    @ApiResponse(responseCode = "404", description = "Item não encontrado")
  })
  @GetMapping("/item/{id}/historico-reposicao")
  public List<Item.HistoricoReposicao> listarHistoricoReposicao(
      @Parameter(description = "ID do item", example = "85", required = true) @PathVariable
          Long id) {
    return itemService.listarHistoricoReposicaoPorItemId(id);
  }

  @Operation(
      summary = "Registrar histórico de reposição",
      description = "Registra uma nova movimentação de reposição para o item.")
  @PostMapping("/item/historico-reposicao")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void registrarHistoricoReposicao(@RequestBody Item.HistoricoReposicao.Request request) {
    itemService.registrarHistoricoReposicao(request);
  }

  @Operation(
      summary = "Atualizar histórico de reposição",
      description = "Atualiza uma movimentação de reposição existente.")
  @PutMapping("/item/historico-reposicao")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void atualizarHistoricoReposicao(@RequestBody Item.HistoricoReposicao.Update request) {
    itemService.atualizarHistoricoReposicao(request);
  }

  @Operation(summary = "Criar categoria")
  @ApiResponse(responseCode = "201", description = "Categoria criada")
  @PostMapping("/categoria")
  @ResponseStatus(HttpStatus.CREATED)
  public Long criarCategoria(@RequestBody CategoriaItem.Request request) {
    return itemService.criarCategoria(request);
  }

  @Operation(summary = "Atualizar categoria")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Categoria atualizada"),
    @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
  })
  @PutMapping("/categoria/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void atualizarCategoria(
      @PathVariable Long id, @RequestBody CategoriaItem.Request request) {
    itemService.atualizarCategoria(id, request);
  }

  @Operation(
      summary = "Buscar categoria por id",
      description = "Retorna uma categoria específica pelo id.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Categoria encontrada"),
    @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
  })
  @GetMapping("/categoria/{id}")
  public CategoriaItem buscarCategoriaPorId(@PathVariable Long id) {
    return itemService.buscarCategoriaPorId(id);
  }

  @Operation(
      summary = "Listar categorias de itens",
      description = "Retorna todas as categorias cadastradas em ordem alfabética.")
  @ApiResponse(
      responseCode = "200",
      description = "Lista de categorias retornada com sucesso",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = CategoriaItem.class))))
  @GetMapping("/categorias")
  public List<CategoriaItem> listarCategorias() {
    return itemService.listarCategorias();
  }
}
