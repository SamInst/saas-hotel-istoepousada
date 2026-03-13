package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.PessoaService;

@Tag(
    name = "Cadastro de Pessoas",
    description = "Endpoints de cadastro e consulta de pessoas (hóspedes/clientes).")
@RestController
@RequestMapping("/pessoa")
@RequireTela("CADASTRO")
public class PessoaController {
  private final PessoaService pessoaService;

  public PessoaController(PessoaService pessoaService) {
    this.pessoaService = pessoaService;
  }

  @Operation(
      summary = "Listar pessoas com filtros opcionais",
      description =
          """
                    Lista pessoas paginadas com filtros opcionais:
                    - id: busca específica por ID
                    - termo: filtra por nome ou CPF
                    - placaVeiculo: filtra por placa vinculada
                    - status: filtra pelo status da pessoa

                    Se nenhum filtro for informado, retorna todas as pessoas paginadas.
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Página de pessoas",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Page.class)))
  })
  @GetMapping
  public Page<Pessoa> listar(
      @Parameter(description = "ID da pessoa") @RequestParam(required = false) Long id,
      @Parameter(description = "Termo para busca por nome ou CPF") @RequestParam(required = false)
          String termo,
      @Parameter(description = "Placa do veículo vinculada à pessoa")
          @RequestParam(required = false)
          String placaVeiculo,
      @Parameter(description = "Status da pessoa", example = "ATIVO")
          @RequestParam(required = false)
          Pessoa.Status status,
      @Parameter(description = "Número da página (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Tamanho da página", example = "10")
          @RequestParam(defaultValue = "10")
          int size) {

    Pageable pageable = PageRequest.of(page, size);
    return pessoaService.buscar(id, termo, placaVeiculo, status, pageable);
  }

  @Operation(
      summary = "Criar pessoas",
      description = "Cria um titular e seus acompanhantes, com vínculos opcionais a empresas.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Pessoas criadas",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = Pessoa.class)))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public List<Pessoa> criar(
      @RequestBody Pessoa.BatchRequest request) {
    return pessoaService.salvarListaPessoas(request);
  }

  @Operation(summary = "Atualizar pessoa", description = "Atualiza os dados de uma pessoa pelo ID.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Pessoa atualizada",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Pessoa.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
  })
  @PutMapping
  public Pessoa atualizar(
          @RequestBody
          Pessoa.Update pessoa) {
    return pessoaService.atualizarPessoa(pessoa);
  }

//  @Operation(
//      summary = "Alterar status da pessoa",
//      description = "Altera o status da pessoa pelo ID.")
//  @ApiResponses({
//    @ApiResponse(responseCode = "204", description = "Status alterado com sucesso"),
//    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
//    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
//  })
//  @PatchMapping("/{id}/status")
//  @ResponseStatus(HttpStatus.NO_CONTENT)
//  public void alterarStatus(
//      @Parameter(description = "ID da pessoa", example = "10", required = true) @PathVariable
//          Long id,
//      @Parameter(description = "Novo status da pessoa", example = "ATIVO") @RequestParam
//          Pessoa.Status status) {
//    pessoaService.alterarStatus(id, status);
//  }

//  @Operation(
//      summary = "Incrementar hospedagem",
//      description = "Incrementa o contador de hospedagens da pessoa.")
//  @ApiResponses({
//    @ApiResponse(responseCode = "204", description = "Hospedagem incrementada com sucesso"),
//    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
//    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
//  })
//  @PatchMapping("/{id}/incrementar-hospedagem")
//  @ResponseStatus(HttpStatus.NO_CONTENT)
//  public void incrementarHospedagem(
//      @Parameter(description = "ID da pessoa", example = "10", required = true) @PathVariable
//          Long id) {
//    pessoaService.incrementarHospedagem(id);
//  }

  //    @Operation(
  //            summary = "Buscar histórico de hospedagem do cliente",
  //            description =
  //                    """
  //                    Busca o histórico de hospedagem por pessoaId.
  //
  //                    Regras de data:
  //                    - Se dataInicio e dataFim não forem informadas, retorna o último histórico
  // mais recente.
  //                    - Se apenas dataInicio for informada, busca desta data em diante.
  //                    - Se apenas dataFim for informada, busca desta data para trás.
  //                    - Se dataInicio e dataFim forem informadas, busca por range com interseção
  // de período.
  //                    """)
  //    @ApiResponses({
  //            @ApiResponse(
  //                    responseCode = "200",
  //                    description = "Histórico encontrado",
  //                    content =
  //                    @Content(
  //                            mediaType = MediaType.APPLICATION_JSON_VALUE,
  //                            schema = @Schema(implementation = HistoricoHospedagem.class))),
  //            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
  //            @ApiResponse(responseCode = "404", description = "Histórico não encontrado")
  //    })
  //    @GetMapping(value = "/historico-hospedagem", produces = MediaType.APPLICATION_JSON_VALUE)
  //    public HistoricoHospedagem buscarHistoricoHospedagem(
  //            @Parameter(description = "ID da pessoa", example = "57", required = true)
  //            @RequestParam
  //            Long pessoaId,
  //            @Parameter(
  //                    description =
  //                            "Data inicial do filtro (yyyy-MM-dd). Se informar somente esta,
  // busca desta data em diante.",
  //                    example = "2026-01-20")
  //            @RequestParam(required = false)
  //            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  //            LocalDate dataInicio,
  //            @Parameter(
  //                    description =
  //                            "Data final do filtro (yyyy-MM-dd). Se informar somente esta, busca
  // desta data para trás.",
  //                    example = "2026-01-23")
  //            @RequestParam(required = false)
  //            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  //            LocalDate dataFim) {
  //        return historicoHospedagemService.buscar(pessoaId, dataInicio, dataFim);
  //    }
}
