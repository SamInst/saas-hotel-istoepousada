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
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.HistoricoHospedagem;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.dto.PessoaBatchRequest;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.HistoricoHospedagemService;
import saas.hotel.istoepousada.service.PessoaService;

@Tag(
    name = "Cadastro de Pessoas",
    description = "Endpoints de cadastro e consulta de pessoas (hóspedes/clientes).")
@RestController
@RequestMapping("/pessoa")
@RequireTela("CADASTRO")
public class PessoaController {
  private final PessoaService pessoaService;
  private final HistoricoHospedagemService historicoHospedagemService;

  public PessoaController(
      PessoaService pessoaService, HistoricoHospedagemService historicoHospedagemService) {
    this.pessoaService = pessoaService;
    this.historicoHospedagemService = historicoHospedagemService;
  }

  @Operation(
      summary = "Listar pessoas (paginado) com filtros opcionais",
      description =
          """
          Lista pessoas paginadas. Filtros são opcionais:
          - id: busca específica por ID
          - termo: filtra por nome (ILIKE) ou CPF (exato)
          - hospedados=true: retorna somente hospedados

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
      @Parameter(description = "Termo para busca por nome (ILIKE) ou CPF exato sem ponto e traço")
          @RequestParam(required = false)
          String termo,
      @Parameter(description = "Termo para busca por placa de veiculo (ILIKE) sem ponto e traço")
          @RequestParam(required = false)
          String placaVeiculo,
      @Parameter(description = "Se true, filtra apenas pessoas hospedadas", example = "true")
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
      description = "Cria um titular (titularId=null) e seus acompanhantes.")
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
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description =
                  "Lista de pessoas (1 titular + N acompanhantes). O servidor define o titularId dos acompanhantes.",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      array = @ArraySchema(schema = @Schema(implementation = Pessoa.class)),
                      examples =
                          @ExampleObject(
                              name = "Exemplo (titular + acompanhantes)",
                              value =
                                  """
                                  {
                                       "pessoas": [
                                           {
                                               "nome": "Titular da Silva",
                                               "dataNascimento": "1999-01-01",
                                               "cpf": "00000000000",
                                               "rg": "000000",
                                               "email": "titular@email.com",
                                               "telefone": "999999999",
                                               "pais": "BR",
                                               "estado": "MA",
                                               "municipio": "São Luís",
                                               "endereco": "Rua X",
                                               "complemento": "Apto 1",
                                               "cep": "65000-000",
                                               "bairro": "Centro",
                                               "sexo": 1,
                                               "numero": "10",
                                               "veiculos": [
                                                   {
                                                       "modelo": "Gol",
                                                       "marca": "VW",
                                                       "ano": 2015,
                                                       "placa": "ABC1D23",
                                                       "cor": "Branco"
                                                   }
                                               ],
                                               "titularId": null
                                           },
                                           {
                                               "nome": "Acompanhante da Silva",
                                               "dataNascimento": "1999-01-01",
                                               "cpf": "00000000000",
                                               "rg": "000000",
                                               "email": "acompanhante@email.com",
                                               "telefone": "999999999",
                                               "pais": "BR",
                                               "estado": "MA",
                                               "municipio": "São Luís",
                                               "endereco": "Rua X",
                                               "complemento": "Apto 1",
                                               "cep": "65000-000",
                                               "bairro": "Centro",
                                               "sexo": 1,
                                               "numero": "10",
                                               "veiculos": [
                                                   {
                                                       "modelo": "Gol 2",
                                                       "marca": "VW",
                                                       "ano": 2015,
                                                       "placa": "ABC1D24",
                                                       "cor": "Branco"
                                                   }
                                               ],
                                               "titularId": 1
                                           }
                                       ],
                                       "empresasIds": [
                                           15
                                       ]
                                   }
        """)))
          @RequestBody
          PessoaBatchRequest request) {
    return pessoaService.salvarListaPessoas(request.pessoas(), request.empresasIds());
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
  @PutMapping("/{id}")
  public Pessoa atualizar(
      @Parameter(description = "ID da pessoa", example = "10", required = true) @PathVariable
          Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados para atualização",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Pessoa.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo de requisição",
                              value =
                                  """
                                            {
                                              "nome": "string",
                                              "dataNascimento": "2025-12-31",
                                              "cpf": "string",
                                              "rg": "string",
                                              "email": "string",
                                              "telefone": "string",
                                              "pais": "string",
                                              "estado": "string",
                                              "municipio": "string",
                                              "endereco": "string",
                                              "complemento": "string",
                                              "cep": "string",
                                              "bairro": "string",
                                              "sexo": 0,
                                              "numero": "string",
                                              "status": "ATIVO",
                                              "veiculos": [
                                                {
                                                  "modelo": "string",
                                                  "marca": "string",
                                                  "ano": 1999,
                                                  "placa": "string",
                                                  "cor": "string"
                                                }
                                              ],
                                              "empresasVinculadas": [{"id": 15 }],
                                              "titularId": 1
                                            }
                                            """)))
          @RequestBody
          Pessoa pessoa) {
    return pessoaService.salvarPessoaIndividual(pessoa.withId(id));
  }

  @Operation(
      summary = "Buscar histórico de hospedagem do cliente",
      description =
          """
                    Busca o histórico de hospedagem por pessoaId.

                    Regras de data:
                    - Se dataInicio e dataFim não forem informadas: retorna o último histórico (mais recente).
                    - Se apenas dataInicio for informada: busca desta data em diante.
                    - Se apenas dataFim for informada: busca desta data para trás.
                    - Se dataInicio e dataFim forem informadas: busca por range (pernoites que tenham interseção/overlap com o período).
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Histórico encontrado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HistoricoHospedagem.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Histórico não encontrado")
  })
  @GetMapping(value = "/historico-hospedagem", produces = MediaType.APPLICATION_JSON_VALUE)
  public HistoricoHospedagem buscar(
      @Parameter(description = "ID da pessoa", example = "57", required = true) @RequestParam
          Long pessoaId,
      @Parameter(
              description =
                  "Data inicial do filtro (yyyy-MM-dd). Se informar somente esta, busca desta data em diante.",
              example = "2026-01-20")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @Parameter(
              description =
                  "Data final do filtro (yyyy-MM-dd). Se informar somente esta, busca desta data para trás.",
              example = "2026-01-23")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim) {
    return historicoHospedagemService.buscar(pessoaId, dataInicio, dataFim);
  }
}
