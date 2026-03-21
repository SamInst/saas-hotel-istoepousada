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
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.HistoricoFuncionarioService;

@Tag(
    name = "Histórico do Funcionário",
    description = "Endpoints de histórico de descricao e salário do funcionário.")
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
      description = "Lista o histórico (descricao e salário) de um funcionário.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de históricos",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Funcionario.Historico.class)))
  })
  @GetMapping
  public List<Funcionario.Historico> listarPorFuncionario(
      @Parameter(description = "ID do funcionário", example = "12", required = true) @RequestParam
          Long funcionarioId) {
    return historicoFuncionarioService.listarPorFuncionario(funcionarioId);
  }

  @Operation(
      summary = "Buscar histórico por uuid",
      description = "Retorna um registro de histórico pelo uuid.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Histórico encontrado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Funcionario.Historico.class))),
    @ApiResponse(responseCode = "404", description = "Histórico não encontrado")
  })
  @GetMapping("/{id}")
  public Funcionario.Historico buscarPorId(
      @Parameter(description = "ID do histórico", example = "1", required = true) @PathVariable
          Long id) {
    return historicoFuncionarioService.buscarPorId(id);
  }

  @Operation(
      summary = "Criar histórico do funcionário",
      description = "Cria um registro de histórico (descricao e salário) para um funcionário.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Histórico criado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Funcionario.Historico.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Funcionario.Historico criar(
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

    return historicoFuncionarioService.insert(
        new Funcionario.Historico.Request(
            new Cargo.Id(cargoId), new Funcionario.Id(funcionarioId), salario));
  }

  @Operation(
      summary = "Atualizar histórico do funcionário",
      description =
          "Atualiza descricao, funcionário e salário de um registro de histórico existente.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Histórico atualizado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Funcionario.Historico.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Histórico não encontrado")
  })
  @PutMapping("/{id}")
  public Funcionario.Historico atualizar(
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

    Funcionario.Historico.Update historico =
        new Funcionario.Historico.Update(id, new Cargo.Id(cargoId), salario);
    return historicoFuncionarioService.update(historico);
  }
}
