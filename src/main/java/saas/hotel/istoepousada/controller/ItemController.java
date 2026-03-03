package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import saas.hotel.istoepousada.dto.*;
import saas.hotel.istoepousada.service.ItemService;

@Tag(
    name = "Itens/Categorias/Almoxarifado",
    description =
        "Endpoints de cadastro/edicao e consulta de itens, categorias e visualizacao do almoxarifado completo..")
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
          """
            Busca itens com filtros opcionais de id, termo, categoria e período de cadastro.
            """)
  @GetMapping("/item/buscar")
  public Page<ItemResponse> buscar(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String termo,
      @RequestParam(required = false) Long categoriaId,
      @RequestParam(required = false) LocalDate dataInicioCadastro,
      @RequestParam(required = false) LocalDate dataFimCadastro,
      Pageable pageable) {

    return itemService.buscar(
        id, termo, categoriaId, dataInicioCadastro, dataFimCadastro, pageable);
  }

  @Operation(summary = "Criar novo item")
  @ApiResponse(responseCode = "201", description = "Item criado")
  @PostMapping("/item")
  @ResponseStatus(HttpStatus.CREATED)
  public ItemResponse criarItem(@RequestBody ItemResponse.ItemRequest request) {
    return itemService.criarItem(request);
  }

  @Operation(summary = "Atualizar item")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Item atualizado"),
    @ApiResponse(responseCode = "404", description = "Item não encontrado")
  })
  @PutMapping("/item/{id}")
  public ItemResponse atualizarItem(
      @PathVariable Long id, @RequestBody ItemResponse.ItemRequest request) {
    return itemService.atualizarItem(id, request);
  }

  @Operation(
      summary = "Dashboard completo de itens",
      description = "Retorna dados agregados de itens e categorias para o dashboard.")
  @GetMapping("/item/dashboard")
  public ItemBuscaCompleta buscarCompleto(
      @RequestParam(required = false) LocalDate dataInicioCadastro,
      @RequestParam(required = false) LocalDate dataFimCadastro) {
    return itemService.buscarCompleto(dataInicioCadastro, dataFimCadastro);
  }

  @Operation(
      summary = "Listar histórico de reposições de um item",
      description =
          "Retorna o histórico de reposições de estoque de um item, com totais por reposição e totais gerais.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Histórico de reposições do item",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HistoricoReposicaoItem.class))),
    @ApiResponse(responseCode = "404", description = "Item não encontrado")
  })
  @GetMapping("/item/{id}/historico-reposicao")
  public HistoricoReposicaoItem listarHistoricoReposicao(
      @Parameter(description = "ID do item", example = "85", required = true) @PathVariable
          Long id) {
    return itemService.listarHistoricoReposicaoPorItemId(id);
  }

  @Operation(
      summary = "Repor estoque de um item",
      description = "Adiciona unidades ao estoque do item, registrando histórico de reposição.")
  @PostMapping("/item/{id}/repor")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reporEstoque(
      @PathVariable Long id,
      @RequestParam Integer quantidade,
      @RequestParam(required = false) Double valorCompraUnidade,
      @RequestParam(required = false) Double valorVendaUnidade,
      @RequestParam(required = false) String fornecedor) {

    itemService.reporEstoque(id, quantidade, valorCompraUnidade, valorVendaUnidade, fornecedor);
  }

  @Operation(
      summary = "Consumir item",
      description = "Remove unidades do estoque do item, registrando histórico de saída.")
  @PostMapping("/item/{id}/consumir")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void consumirEstoque(@PathVariable Long id, @RequestParam int quantidade) {

    itemService.consumirEstoque(id, quantidade);
  }

  @Operation(
      summary = "Listar histórico de preços de um item",
      description = "Retorna o histórico de preço de um item, em ordem decrescente de data.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Histórico de preço do item",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HistoricoPrecoItem.class))),
    @ApiResponse(responseCode = "404", description = "Item não encontrado")
  })
  @GetMapping("/item/{id}/historico-preco")
  public List<HistoricoPrecoItem> listarHistoricoPreco(
      @Parameter(description = "ID do item", example = "85", required = true) @PathVariable
          Long id) {
    return itemService.listarHistoricoPrecoPorItemId(id);
  }

  @Operation(summary = "Criar categoria")
  @PostMapping("/categoria")
  @ResponseStatus(HttpStatus.CREATED)
  public Long criarCategoria(
      @RequestParam String categoria, @RequestParam(required = false) String descricao) {
    return itemService.criarCategoria(categoria, descricao);
  }

  @Operation(summary = "Atualizar categoria")
  @PutMapping("/categoria/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void atualizarCategoria(
      @PathVariable Long id,
      @RequestParam String categoria,
      @RequestParam(required = false) String descricao) {
    itemService.atualizarCategoria(id, categoria, descricao);
  }

  @Operation(
      summary = "Listar categorias de itens",
      description = "Retorna todas as categorias cadastradas, em ordem alfabética.")
  @ApiResponse(
      responseCode = "200",
      description = "Lista de categorias retornada com sucesso",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ItemCategoria.class)))
  @GetMapping("/categorias")
  public List<ItemCategoria> listarCategorias() {
    return itemService.listarCategorias();
  }
}
