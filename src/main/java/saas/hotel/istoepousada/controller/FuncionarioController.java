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
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.FuncionarioService;

@Tag(name = "Funcionários", description = "Endpoints de cadastro e consulta de funcionários.")
@RestController
@RequestMapping("/funcionario")
@RequireTela("ADMIN")
public class FuncionarioController {
  private final FuncionarioService funcionarioService;

  public FuncionarioController(FuncionarioService funcionarioService) {
    this.funcionarioService = funcionarioService;
  }

  @Operation(
      summary = "Listar funcionários (paginado) com filtros opcionais",
      description =
          """
                    Lista funcionários paginados. Filtros são opcionais:
                    - id: busca específica por ID do funcionário
                    - termo: filtra por nome (ILIKE) ou CPF (exato)
                    - cargoId: filtra por cargo específico
                    - pessoaId: filtra por ID da pessoa vinculada
                    - usuarioId: filtra por ID do usuário vinculado

                    Se nenhum filtro for informado, retorna todos os funcionários contratados paginados.
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Página de funcionários",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Page.class)))
  })
  @GetMapping
  public Page<Funcionario> listar(
      @Parameter(description = "ID do funcionário") @RequestParam(required = false) Long id,
      @Parameter(description = "Termo para busca por nome (ILIKE) ou CPF exato sem ponto e traço")
          @RequestParam(required = false)
          String termo,
      @Parameter(description = "ID do cargo", example = "1") @RequestParam(required = false)
          Long cargoId,
      @Parameter(description = "ID da pessoa", example = "57") @RequestParam(required = false)
          Long pessoaId,
      @Parameter(description = "ID do usuário", example = "10") @RequestParam(required = false)
          Long usuarioId,
      @Parameter(description = "Número da página (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Tamanho da página", example = "10")
          @RequestParam(defaultValue = "10")
          int size) {
    Pageable pageable = PageRequest.of(page, size);
    return funcionarioService.search(id, termo, cargoId, pessoaId, usuarioId, pageable);
  }

  @Operation(
      summary = "Cadastrar funcionário",
      description =
          """
                    Cadastra um novo funcionário no sistema.

                    Processo:
                    1. Valida se a pessoa existe
                    2. Altera o status da pessoa para CONTRATADO
                    3. Cria usuário se os dados forem fornecidos (opcional)
                    4. Vincula pessoa, cargo e usuário ao funcionário
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Funcionário criado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Funcionario.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Funcionario criar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados do funcionário",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Funcionario.FuncionarioRequest.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo completo",
                              value =
                                  """
                                            {
                                              "pessoaId": 57,
                                              "dataAdmissao": "2026-01-19",
                                              "salario": 6543.21,
                                              "cargoId": 1,
                                              "usuario": {
                                                "username": "joao.silva",
                                                "senha": "senha123"
                                              }
                                            }
                                            """)))
          @RequestBody
          Funcionario.FuncionarioRequest request) {
    return funcionarioService.create(request);
  }

  @Operation(
      summary = "Atualizar funcionário",
      description =
          """
                    Atualiza os dados de um funcionário existente.

                    Campos atualizáveis:
                    - cargoId: Altera o cargo do funcionário (caso null, sera removido o cargo do funcionario)
                    - dataAdmissao: Corrige a data de admissão
                    - salario: Atualiza o salário

                    Nota: O vínculo com a pessoa (pessoaId) não pode ser alterado.
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Funcionário atualizado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Funcionario.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Funcionário não encontrado")
  })
  @PutMapping("/{id}")
  public Funcionario atualizar(
      @Parameter(description = "ID do funcionário", example = "1", required = true) @PathVariable
          Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados para atualização do funcionário",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Funcionario.FuncionarioRequest.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo de atualização",
                              value =
                                  """
                                            {
                                              "cargoId": 2,
                                              "dataAdmissao": "2026-01-15",
                                              "salario": 7500.00
                                            }
                                            """)))
          @RequestBody
          Funcionario.FuncionarioRequest request) {
    return funcionarioService.update(id, request);
  }
}
