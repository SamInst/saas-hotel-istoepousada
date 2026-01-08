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
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.service.EmpresaService;

@Tag(
    name = "Empresas",
    description = "Cadastro e consulta de empresas (parceiros, clientes PJ) e vínculo com pessoas.")
@RestController
@RequestMapping("/empresas")
public class EmpresaController {

  private final EmpresaService empresaService;

  public EmpresaController(EmpresaService empresaService) {
    this.empresaService = empresaService;
  }

  @Operation(
      summary = "Listar empresas (paginado) com filtros opcionais",
      description =
          """
          Lista empresas paginadas. Filtros são opcionais:
          - id: busca específica por ID
          - termo: filtra por razão social/nome fantasia (ILIKE) ou CNPJ (exato)

          Se nenhum filtro for informado, retorna todas as empresas paginadas.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Página de empresas",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Page.class)))
  })
  @GetMapping
  public Page<Empresa> listar(
      @Parameter(description = "ID da empresa", example = "1") @RequestParam(required = false)
          Long id,
      @Parameter(
              description = "Termo para busca por razão social/nome fantasia (ILIKE) ou CNPJ exato",
              example = "ISTOÉ")
          @RequestParam(required = false)
          String termo,
      @Parameter(description = "Número da página (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Tamanho da página", example = "10")
          @RequestParam(defaultValue = "10")
          int size) {
    Pageable pageable = PageRequest.of(page, size);
    return empresaService.buscarPorIdNomeOuCnpj(id, termo, pageable);
  }

  @Operation(
      summary = "Criar empresa",
      description = "Cria uma nova empresa (cliente PJ ou parceiro).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Empresa criada",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Empresa.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Empresa criar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados da empresa",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Empresa.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo de criação de empresa",
                              summary = "Payload de criação",
                              value =
                                  """
                                            {
                                              "cnpj": "12.345.678/0001-90",
                                              "telefone": "(98) 99999-9999",
                                              "email": "contato@empresa.com",
                                              "endereco": "Rua das Flores",
                                              "cep": "65000-000",
                                              "numero": "123",
                                              "complemento": "Sala 101",
                                              "pais": "Brasil",
                                              "estado": "MA",
                                              "municipio": "São Luís",
                                              "bairro": "Centro",
                                              "bloqueado": false,
                                              "razao_social": "Empresa Exemplo LTDA",
                                              "nome_fantasia": "Empresa Exemplo",
                                              "inscricao_estadual": "123456789",
                                              "inscricao_municipal": "987654321",
                                              "tipo_empresa": "CLIENTE"
                                            }
                                            """)))
          @RequestBody
          Empresa empresa) {

    return empresaService.salvar(empresa);
  }

  @Operation(
      summary = "Atualizar empresa",
      description = "Atualiza os dados de uma empresa pelo ID informado no path.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Empresa atualizada",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Empresa.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
  })
  @PutMapping("/{id}")
  public Empresa atualizar(
      @Parameter(description = "ID da empresa", example = "1", required = true) @PathVariable
          Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados para atualização da empresa (ID informado apenas no path)",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Empresa.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo de atualização",
                              summary = "Payload de atualização da empresa",
                              value =
                                  """
                                            {
                                              "cnpj": "12.345.678/0001-90",
                                              "telefone": "(98) 99999-9999",
                                              "email": "contato@empresa.com",
                                              "endereco": "Rua das Flores",
                                              "cep": "65000-000",
                                              "numero": "123",
                                              "complemento": "Sala 101",
                                              "pais": "Brasil",
                                              "estado": "MA",
                                              "municipio": "São Luís",
                                              "bairro": "Centro",
                                              "bloqueado": false,
                                              "razao_social": "Empresa Exemplo LTDA",
                                              "nome_fantasia": "Empresa Exemplo",
                                              "inscricao_estadual": "123456789",
                                              "inscricao_municipal": "987654321",
                                              "tipo_empresa": "CLIENTE"
                                            }
                                            """)))
          @RequestBody
          Empresa empresa) {

    return empresaService.salvar(empresa.withId(id));
  }

  @Operation(
      summary = "Vincular ou desvincular pessoa à empresa",
      description =
          """
                    Vincula ou desvincula uma pessoa a uma empresa.

                    - vinculo=true: vincula (padrão)
                    - vinculo=false: desvincula
                    """)
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Operação realizada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Empresa ou pessoa não encontrada")
  })
  @PostMapping("/{empresaId}/pessoa")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void vincularOuDesvincularPessoa(
      @Parameter(description = "ID da empresa", example = "1", required = true) @PathVariable
          Long empresaId,
      @Parameter(
              description = "ID da pessoa a ser vinculada ou desvinculada",
              example = "10",
              required = true)
          @RequestParam(name = "pessoaId", required = true)
          Long pessoaId,
      @Parameter(description = "true para vincular, false para desvincular", example = "true")
          @RequestParam(name = "vinculo", defaultValue = "true")
          Boolean vinculo) {

    empresaService.vincularPessoa(empresaId, pessoaId, vinculo);
  }
}
