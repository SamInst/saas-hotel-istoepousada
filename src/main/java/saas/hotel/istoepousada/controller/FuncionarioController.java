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
import saas.hotel.istoepousada.service.FuncionarioService;

@Tag(name = "Funcionários", description = "Endpoints de cadastro e consulta de funcionários.")
@RestController
@RequestMapping("/funcionario")
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
                    - id: busca específica por ID
                    - termo: filtra por nome (ILIKE) ou CPF (exato)
                    - cargoId: filtra por cargo específico

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
      @Parameter(description = "Número da página (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Tamanho da página", example = "10")
          @RequestParam(defaultValue = "10")
          int size) {
    Pageable pageable = PageRequest.of(page, size);
    return funcionarioService.buscar(id, termo, cargoId, pageable);
  }

  @Operation(
      summary = "Criar funcionário",
      description =
          """
                    Cria um novo funcionário no sistema.

                    Processo:
                    1. Cria/atualiza a pessoa com os dados fornecidos
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
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
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
                                              "pessoa": {
                                                "nome": "João Silva Santos",
                                                "dataNascimento": "1990-05-15",
                                                "cpf": "12345678900",
                                                "rg": "MG1234567",
                                                "email": "joao.silva@hotel.com",
                                                "telefone": "(31) 98765-4321",
                                                "pais": "Brasil",
                                                "estado": "MG",
                                                "municipio": "Belo Horizonte",
                                                "endereco": "Rua das Flores",
                                                "numero": "123",
                                                "complemento": "Apto 201",
                                                "bairro": "Centro",
                                                "cep": "30000-000",
                                                "sexo": 1
                                              },
                                              "dataAdmissao": "2026-01-19",
                                              "cargoId": 1,
                                              "usuario": {
                                                "username": "joao.silva",
                                                "senha": "senha123"
                                              }
                                            }
                                            """)))
          @RequestBody
          Funcionario.FuncionarioRequest request) {
    return funcionarioService.criar(request);
  }
}
