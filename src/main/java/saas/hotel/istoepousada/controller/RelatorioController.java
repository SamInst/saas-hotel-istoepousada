package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Relatorio;
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
                            - uuid: busca específica por ID (mesclada na busca global)
                            - dataInicio: filtra a partir da data (>= 00:00)
                            - dataFim: filtra até a data (< próximo dia 00:00)
                            - funcionarioId: ID da pessoa (funcionário responsável)
                            - quartoId: ID do quarto (opcional)
                            - tipoPagamentoId: ID do tipo de pagamento
                            - registro: ENTRADA (valor > 0) ou SAIDA (valor < 0)
                            - despesaPessoal: se true, filtra apenas despesas pessoais

                            A resposta contém:
                            - pagamentos: mapa com totais por tipo de pagamento (inclui chave \"TOTAL\")
                            - page: página de relatórios agrupados por dia
                            """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Extrato de relatórios com totais por tipo de pagamento",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Relatorio.Extrato.class)))
  })
  @GetMapping
  public Relatorio.Extrato listar(
      @Parameter(description = "ID do relatório") @RequestParam(required = false) Long id,
      @Parameter(description = "Data inicial (yyyy-MM-dd)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @Parameter(description = "Data final (yyyy-MM-dd)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
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
          Relatorio.Registro registro,
      @Parameter(
              description =
                  "Se informado, filtra por despesa pessoal (true) ou não pessoal (false).",
              example = "true")
          @RequestParam(required = false)
          Boolean despesaPessoal,
      @Parameter(description = "Número da página (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Tamanho da página", example = "10")
          @RequestParam(defaultValue = "10")
          int size) {

    Pageable pageable = PageRequest.of(page, size);
    return relatorioService.buscar(
        id,
        dataInicio,
        dataFim,
        funcionarioId,
        quartoId,
        tipoPagamentoId,
        registro,
        despesaPessoal,
        pageable);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Relatorio criar(@RequestBody Relatorio.Request request) {
    return relatorioService.criar(request);
  }

  @PutMapping
  public Relatorio atualizar(@RequestBody Relatorio.Update relatorio) {
    return relatorioService.atualizar(relatorio);
  }
}
