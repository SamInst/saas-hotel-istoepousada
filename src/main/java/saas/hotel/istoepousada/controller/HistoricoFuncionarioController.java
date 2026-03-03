package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.HistoricoFuncionario;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.HistoricoFuncionarioService;

@Tag(
    name = "Histórico do Funcionário",
    description = "Endpoints de histórico de cargo e salário do funcionário.")
@RestController
@RequestMapping("/historico-funcionario")
@RequireTela("ADMIN")
public class HistoricoFuncionarioController {

  private final HistoricoFuncionarioService historicoFuncionarioService;

  public HistoricoFuncionarioController(HistoricoFuncionarioService historicoFuncionarioService) {
    this.historicoFuncionarioService = historicoFuncionarioService;
  }

  @Operation(
      summary = "Listar histórico por funcionário",
      description = "Lista o histórico (cargo e salário) de um funcionário.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de históricos",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HistoricoFuncionario.class)))
  })
  @GetMapping
  public List<HistoricoFuncionario> listarPorFuncionario(
      @Parameter(description = "ID do funcionário", example = "12", required = true) @RequestParam
          Long funcionarioId) {
    return historicoFuncionarioService.listarPorFuncionario(funcionarioId);
  }

  @Operation(
      summary = "Buscar histórico por id",
      description = "Retorna um registro de histórico pelo id.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Histórico encontrado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HistoricoFuncionario.class))),
    @ApiResponse(responseCode = "404", description = "Histórico não encontrado")
  })
  @GetMapping("/{id}")
  public HistoricoFuncionario buscarPorId(
      @Parameter(description = "ID do histórico", example = "1", required = true) @PathVariable
          Long id) {
    return historicoFuncionarioService.buscarPorId(id);
  }

  @Operation(
      summary = "Criar histórico do funcionário",
      description = "Cria um registro de histórico (cargo e salário) para um funcionário.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Histórico criado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HistoricoFuncionario.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public HistoricoFuncionario criar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados do histórico",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(
                              name = "Exemplo de requisição",
                              value =
                                  """
                                            {
                                              "cargoId": 3,
                                              "funcionarioId": 12,
                                              "salario": 2500.00
                                            }
                                            """)))
          @RequestBody
          Map<String, Object> body) {

    Long cargoId = body.get("cargoId") == null ? null : ((Number) body.get("cargoId")).longValue();
    Long funcionarioId =
        body.get("funcionarioId") == null ? null : ((Number) body.get("funcionarioId")).longValue();
    Float salario =
        body.get("salario") == null ? null : ((Number) body.get("salario")).floatValue();

    HistoricoFuncionario historico =
        new HistoricoFuncionario(
            null,
            new HistoricoFuncionario.Cargo(cargoId, null),
            new HistoricoFuncionario.Funcionario(funcionarioId, null),
            salario);

    return historicoFuncionarioService.salvar(historico);
  }

  @Operation(
      summary = "Atualizar histórico do funcionário",
      description = "Atualiza cargo, funcionário e salário de um registro de histórico existente.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Histórico atualizado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HistoricoFuncionario.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Histórico não encontrado")
  })
  @PutMapping("/{id}")
  public HistoricoFuncionario atualizar(
      @Parameter(description = "ID do histórico", example = "1", required = true) @PathVariable
          Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados do histórico",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(
                              name = "Exemplo de requisição",
                              value =
                                  """
                                            {
                                              "cargoId": 4,
                                              "funcionarioId": 12,
                                              "salario": 3200.00
                                            }
                                            """)))
          @RequestBody
          Map<String, Object> body) {

    Long cargoId = body.get("cargoId") == null ? null : ((Number) body.get("cargoId")).longValue();
    Long funcionarioId =
        body.get("funcionarioId") == null ? null : ((Number) body.get("funcionarioId")).longValue();
    Float salario =
        body.get("salario") == null ? null : ((Number) body.get("salario")).floatValue();

    HistoricoFuncionario historico =
        new HistoricoFuncionario(
            id,
            new HistoricoFuncionario.Cargo(cargoId, null),
            new HistoricoFuncionario.Funcionario(funcionarioId, null),
            salario);

    return historicoFuncionarioService.salvar(historico);
  }
}
