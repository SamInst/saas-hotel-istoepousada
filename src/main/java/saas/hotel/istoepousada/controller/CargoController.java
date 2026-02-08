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
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.dto.Objeto;
import saas.hotel.istoepousada.repository.ObjectRepository;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.CargoService;

@Tag(
    name = "Cargos, Telas e Permissões",
    description = "Endpoints para gerenciamento de cargos, telas e permissões.")
@RestController
@RequestMapping
@RequireTela("ADMIN")
public class CargoController {

  private final CargoService cargoService;
  private final ObjectRepository objectRepository;

  public CargoController(CargoService cargoService, ObjectRepository objectRepository) {
    this.cargoService = cargoService;
    this.objectRepository = objectRepository;
  }

  @Operation(
      summary = "Listar cargos (paginado) com filtros opcionais",
      description =
          """
                    Lista cargos paginados. Filtros são opcionais:
                    - id: busca específica por ID
                    - termo: filtra por nome/descrição do cargo (ILIKE)
                    - pessoaId: filtra cargos vinculados a uma pessoa específica (através da tabela funcionario)

                    Se nenhum filtro for informado, retorna todos os cargos paginados.
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Página de cargos",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Page.class)))
  })
  @GetMapping("/cargo")
  public Page<Cargo> listar(
      @Parameter(description = "ID do cargo") @RequestParam(required = false) Long id,
      @Parameter(description = "Termo para busca por nome/descrição do cargo (ILIKE)")
          @RequestParam(required = false)
          String termo,
      @Parameter(description = "ID da pessoa para filtrar cargos vinculados via funcionario")
          @RequestParam(required = false)
          Long pessoaId,
      @Parameter(description = "Número da página (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Tamanho da página", example = "10")
          @RequestParam(defaultValue = "10")
          int size) {
    Pageable pageable = PageRequest.of(page, size);
    return cargoService.listar(id, termo, pessoaId, pageable);
  }

  @Operation(
      summary = "Criar cargo",
      description =
          """
                    Cria um novo cargo e opcionalmente vincula telas e permissões.
                    O ID não deve ser informado (será gerado automaticamente).
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Cargo criado com sucesso",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Cargo.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
  })
  @PostMapping("/cargo")
  @ResponseStatus(HttpStatus.CREATED)
  public Cargo criar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados do cargo. O campo 'id' deve ser null ou omitido.",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Cargo.Request.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo de criação",
                              value =
                                  """
                                            {
                                              "descricao": "Gerente",
                                              "telasIds": [1, 2, 5],
                                              "permissoesIds": [1, 3, 7, 12]
                                            }
                                            """)))
          @RequestBody
          Cargo.Request request) {
    return cargoService.criar(request);
  }

  @Operation(
      summary = "Atualizar cargo",
      description =
          """
                    Atualiza os dados de um cargo pelo ID.

                    Comportamento dos vínculos:
                    - Se telasIds for informado: substitui TODAS as telas vinculadas pelas novas
                    - Se telasIds for null/omitido: mantém as telas atuais
                    - Se permissoesIds for informado: substitui TODAS as permissões vinculadas pelas novas
                    - Se permissoesIds for null/omitido: mantém as permissões atuais
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Cargo atualizado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Cargo.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Cargo não encontrado")
  })
  @PutMapping("/cargo/{id}")
  public Cargo atualizar(
      @Parameter(description = "ID do cargo", example = "1", required = true) @PathVariable Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados para atualização do cargo",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Cargo.Request.class),
                      examples = {
                        @ExampleObject(
                            name = "Atualizar apenas descrição",
                            value =
                                """
                                                    {
                                                      "descricao": "Gerente Geral"
                                                    }
                                                    """),
                        @ExampleObject(
                            name = "Atualizar descrição e telas",
                            value =
                                """
                                                    {
                                                      "descricao": "Gerente Geral",
                                                      "telasIds": [1, 2, 3, 5]
                                                    }
                                                    """),
                        @ExampleObject(
                            name = "Atualizar tudo",
                            value =
                                """
                                                    {
                                                      "descricao": "Gerente Geral",
                                                      "telasIds": [1, 2, 3, 5],
                                                      "permissoesIds": [1, 3, 5, 7, 12]
                                                    }
                                                    """)
                      }))
          @RequestBody
          Cargo.Request request) {
    return cargoService.atualizar(id, request);
  }

  @Operation(
      summary = "Deletar cargo",
      description =
          """
                    Deleta um cargo pelo ID.
                    Remove automaticamente todos os vínculos com telas e permissões.
                    """)
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Cargo deletado com sucesso"),
    @ApiResponse(responseCode = "404", description = "Cargo não encontrado")
  })
  @DeleteMapping("/cargo/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletar(
      @Parameter(description = "ID do cargo", example = "1", required = true) @PathVariable
          Long id) {
    cargoService.deletar(id);
  }

  @Operation(
      summary = "Vincular ou desvincular telas de um cargo",
      description =
          """
                    Vincula (vinculo=true) ou desvincula (vinculo=false) telas específicas a um cargo.
                    Útil para ajustar permissões pontuais sem precisar reenviar todas as telas.
                    """)
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Operação realizada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Cargo não encontrado")
  })
  @PatchMapping("/cargo/{cargoId}/telas")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void vincularTelas(
      @Parameter(description = "ID do cargo", example = "1", required = true) @PathVariable
          Long cargoId,
      @Parameter(
              description = "Se true, vincula. Se false, desvincula.",
              example = "true",
              required = true)
          @RequestParam
          Boolean vinculo,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Lista de IDs das telas",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(name = "Exemplo de requisição", value = "[1, 2, 5, 8]")))
          @RequestBody
          List<Long> telaIds) {
    cargoService.vincularTelas(cargoId, telaIds, vinculo);
  }

  @Operation(
      summary = "Vincular ou desvincular permissões de um cargo",
      description =
          """
                    Vincula (vinculo=true) ou desvincula (vinculo=false) permissões específicas a um cargo.
                    Permite ajustar permissões pontuais sem precisar reenviar todas as permissões.
                    """)
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Operação realizada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Cargo não encontrado")
  })
  @PatchMapping("/cargo/{cargoId}/permissoes")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void vincularPermissoes(
      @Parameter(description = "ID do cargo", example = "1", required = true) @PathVariable
          Long cargoId,
      @Parameter(
              description = "Se true, vincula. Se false, desvincula.",
              example = "true",
              required = true)
          @RequestParam
          Boolean vinculo,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Lista de IDs das permissões",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(name = "Exemplo de requisição", value = "[1, 3, 7, 12]")))
          @RequestBody
          List<Long> permissaoIds) {
    cargoService.vincularPermissoes(cargoId, permissaoIds, vinculo);
  }

  @Operation(
      summary = "Listar telas do sistema",
      description =
          """
                            Retorna a lista de telas cadastradas (menu/rotas funcionais do sistema).
                            Normalmente usado no front para:
                            - exibir cadastro/edição de permissões
                            - associar permissões a uma tela
                            - montar regras de acesso baseadas em tela

                            Observação: este endpoint é apenas de leitura.
                            """)
  @ApiResponse(
      responseCode = "200",
      description = "Lista de telas",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = Objeto.class)),
              examples =
                  @ExampleObject(
                      name = "Exemplo",
                      value =
                          """
                                            [
                                              { "id": 1, "descricao": "DASHBOARD" },
                                              { "id": 2, "descricao": "CADASTRO" },
                                              { "id": 3, "descricao": "FINANCEIRO" },
                                              { "id": 4, "descricao": "RESERVAS" }
                                            ]
                                            """)))
  @GetMapping("/telas")
  public List<Objeto> telas() {
    return objectRepository.telas();
  }

  @Operation(
      summary = "Listar permissões do sistema por tela id",
      description =
          """
                            Retorna a lista de permissões cadastradas.
                            Permissões são regras granulares dentro de uma tela (ex.: "RELATORIO_CRIAR", "RESERVA_CANCELAR").

                            Normalmente usado no front para:
                            - tela de administração de permissões (associar permissões a pessoas)
                            - construir checkboxes / multi-select
                            - exibir quais permissões existem no sistema

                            Observação: este endpoint é apenas de leitura.
                            """)
  @ApiResponse(
      responseCode = "200",
      description = "Lista de permissões",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = Objeto.class)),
              examples =
                  @ExampleObject(
                      name = "Exemplo",
                      value =
                          """
                                            [
                                              { "id": 10, "descricao": "RELATORIO_VISUALIZAR" },
                                              { "id": 11, "descricao": "RELATORIO_CRIAR" },
                                              { "id": 12, "descricao": "RELATORIO_EDITAR" },
                                              { "id": 20, "descricao": "RESERVA_CRIAR" },
                                              { "id": 21, "descricao": "RESERVA_CANCELAR" }
                                            ]
                                            """)))
  @GetMapping("/permissoes")
  public List<Objeto> permissoes(@RequestParam Long telaId) {
    return objectRepository.permissoes(telaId);
  }
}
