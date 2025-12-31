package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.service.PessoaService;

@Tag(
    name = "Pessoas",
    description = "Endpoints de cadastro e consulta de pessoas (hóspedes/clientes).")
@RestController
@RequestMapping("/pessoa")
public class PessoaController {

  private final PessoaService pessoaService;

  public PessoaController(PessoaService pessoaService) {
    this.pessoaService = pessoaService;
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
      @Parameter(description = "ID da pessoa", example = "10") @RequestParam(required = false)
          Long id,
      @Parameter(
              description = "Termo para busca por nome (ILIKE) ou CPF exato sem ponto e traço",
              example = "João | 12345678910")
          @RequestParam(required = false)
          String termo,
      @Parameter(description = "Se true, filtra apenas pessoas hospedadas", example = "true")
          @RequestParam(required = false)
          Boolean hospedados,
      @Parameter(description = "Número da página (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Tamanho da página", example = "10")
          @RequestParam(defaultValue = "10")
          int size) {
    Pageable pageable = PageRequest.of(page, size);
    return pessoaService.buscar(id, termo, hospedados, pageable);
  }

  @Operation(summary = "Criar pessoa", description = "Cria uma nova pessoa (hóspede/cliente).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Pessoa criada",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Pessoa.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Pessoa criar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados da pessoa",
              required = true,
              content =
                  @Content(
                      mediaType = org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
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
                                            "fkPais": 1,
                                            "fkEstado": 1,
                                            "fkMunicipio": 1,
                                            "endereco": "string",
                                            "complemento": "string",
                                            "hospedado": true,
                                            "cep": "string",
                                            "bairro": "string",
                                            "sexo": 1,
                                            "numero": "string",
                                            "bloqueado": true,
                                            "empresasVinculadas": [
                                              { "id": 1 }
                                            ],
                                            "veiculos": [
                                              {
                                                "modelo": "string",
                                                "marca": "string",
                                                "ano": 1999,
                                                "placa": "string",
                                                "cor": "string"
                                              }
                                            ]
                                          }
                                          """)))
          @RequestBody
          Pessoa pessoa) {
    return pessoaService.salvar(pessoa);
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
                                              "fkPais": 0,
                                              "fkEstado": 0,
                                              "fkMunicipio": 0,
                                              "endereco": "string",
                                              "complemento": "string",
                                              "cep": "string",
                                              "bairro": "string",
                                              "sexo": 0,
                                              "numero": "string",
                                              "bloqueado": true
                                            }
                                            """)))
          @RequestBody
          Pessoa pessoa) {
    return pessoaService.salvar(pessoa.withId(id));
  }
}
