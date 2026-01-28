package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.dto.RelatorioExtratoResponse;
import saas.hotel.istoepousada.dto.enums.Valores;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.RelatorioService;

@Tag(
    name = "Relatórios",
    description = "Endpoints de lançamento e consulta de relatórios financeiros/operacionais.")
@RestController
@RequestMapping("/relatorios")
@RequireTela("FINANCEIRO")
public class RelatorioController {

  private final RelatorioService relatorioService;

  public RelatorioController(RelatorioService relatorioService) {
    this.relatorioService = relatorioService;
  }

  @Operation(
      summary = "Listar relatórios (paginado) com filtros opcionais",
      description =
          """
                    Lista relatórios paginados. Filtros são opcionais:
                    - id: busca específica por ID (mesclada na busca global)
                    - dataInicio: filtra a partir da data/hora (>=)
                    - dataFim: filtra até a data/hora (<=)
                    - funcionarioId: ID da pessoa (funcionário responsável)
                    - quartoId: ID do quarto (opcional)
                    - tipoPagamentoId: ID do tipo de pagamento
                    - valores: ENTRADA (valor > 0) ou SAIDA (valor < 0)

                    Se o ID for informado, a busca retorna somente o registro correspondente (paginado).
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Página de relatórios",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Page.class)))
  })
  @GetMapping
  public RelatorioExtratoResponse listar(
      @Parameter(description = "ID do relatório") @RequestParam(required = false) Long id,
      @Parameter(description = "Data/hora inicial (yyyy-MM-dd'T'HH:mm:ss)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDate dataInicio,
      @Parameter(description = "Data/hora final (yyyy-MM-dd'T'HH:mm:ss)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDate dataFim,
      @Parameter(description = "ID da pessoa (funcionário responsável)")
          @RequestParam(required = false)
          Long funcionarioId,
      @Parameter(description = "ID do quarto") @RequestParam(required = false) Long quartoId,
      @Parameter(description = "ID do tipo de pagamento") @RequestParam(required = false)
          Long tipoPagamentoId,
      @Parameter(
              description = "Filtro por tipo de valor: ENTRADA (valor > 0) ou SAIDA (valor < 0)",
              example = "ENTRADA")
          @RequestParam(required = false)
          Valores valores,
      @Parameter(description = "Número da página (0-based)") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    return relatorioService.buscar(
        id, dataInicio, dataFim, funcionarioId, quartoId, tipoPagamentoId, valores, pageable);
  }

  @Operation(
      summary = "Criar relatório",
      description =
          """
                    Cria um novo relatório financeiro/operacional.

                    O funcionário responsável é identificado automaticamente a partir do usuário logado
                    (e gravado em relatorio.fk_funcionario apontando para pessoa.id).
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Relatório criado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Relatorio.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Relatorio criar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados do relatório",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Relatorio.RelatorioRequest.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo de criação",
                              value =
                                  """
                                            {
                                              "relatorio": "Pagamento de diária",
                                              "valor": 250.00,
                                              "tipoPagamentoId": 1,
                                              "quartoId": 5
                                            }
                                            """)))
          @RequestBody
          Relatorio.RelatorioRequest request) {
    return relatorioService.criar(request);
  }

  @Operation(
      summary = "Atualizar relatório",
      description =
          """
                    Atualiza um relatório existente pelo ID.

                    Observação: o responsável (fk_funcionario) é gravado como a pessoa do usuário logado,
                    mantendo o padrão de auditoria do sistema.
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Relatório atualizado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Relatorio.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Relatório não encontrado")
  })
  @PutMapping("/{id}")
  public Relatorio atualizar(
      @Parameter(description = "ID do relatório", example = "10", required = true) @PathVariable
          Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados para atualização",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Relatorio.RelatorioRequest.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo de atualização",
                              value =
                                  """
                                            {
                                              "relatorio": "Ajuste de valor da diária",
                                              "valor": -230.00,
                                              "tipoPagamentoId": 2,
                                              "quartoId": 5
                                            }
                                            """)))
          @RequestBody
          Relatorio.RelatorioRequest request) {

    return relatorioService.atualizar(id, request);
  }
}
