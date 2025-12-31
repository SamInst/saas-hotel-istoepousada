package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.service.EmpresaService;

import java.util.List;

@Tag(
        name = "Empresas",
        description = "Cadastro e consulta de empresas (parceiros, clientes PJ) e vínculo com pessoas."
)
@RestController
@RequestMapping("/empresas")
public class EmpresaController {

  private final EmpresaService empresaService;

  public EmpresaController(EmpresaService empresaService) {
    this.empresaService = empresaService;
  }

  @Operation(
          summary = "Listar empresas (paginado) com filtros opcionais",
          description = """
          Lista empresas paginadas. Filtros são opcionais:
          - id: busca específica por ID
          - termo: filtra por razão social/nome fantasia (ILIKE) ou CNPJ (exato)

          Se nenhum filtro for informado, retorna todas as empresas paginadas.
          """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Página de empresas",
                  content = @Content(
                          mediaType = MediaType.APPLICATION_JSON_VALUE,
                          schema = @Schema(implementation = Page.class)
                  )
          )
  })
  @GetMapping
  public Page<Empresa> listar(
          @Parameter(description = "ID da empresa", example = "1")
          @RequestParam(required = false) Long id,

          @Parameter(description = "Termo para busca por razão social/nome fantasia (ILIKE) ou CNPJ exato", example = "ISTOÉ")
          @RequestParam(required = false) String termo,

          @Parameter(description = "Número da página (0-based)", example = "0")
          @RequestParam(defaultValue = "0") int page,

          @Parameter(description = "Tamanho da página", example = "10")
          @RequestParam(defaultValue = "10") int size
  ) {
    Pageable pageable = PageRequest.of(page, size);
    return empresaService.buscarPorIdNomeOuCnpj(id, termo, pageable);
  }

  @Operation(
          summary = "Criar empresa",
          description = "Cria uma nova empresa (cliente PJ/parceiro)."
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "201",
                  description = "Empresa criada",
                  content = @Content(
                          mediaType = MediaType.APPLICATION_JSON_VALUE,
                          schema = @Schema(implementation = Empresa.class)
                  )
          ),
          @ApiResponse(responseCode = "400", description = "Requisição inválida")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Empresa criar(
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
                  description = "Dados da empresa",
                  required = true,
                  content = @Content(schema = @Schema(implementation = Empresa.class))
          )
          @RequestBody Empresa empresa
  ) {
    return empresaService.salvar(empresa);
  }

  @Operation(
          summary = "Atualizar empresa",
          description = "Atualiza os dados de uma empresa pelo ID."
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Empresa atualizada",
                  content = @Content(
                          mediaType = MediaType.APPLICATION_JSON_VALUE,
                          schema = @Schema(implementation = Empresa.class)
                  )
          ),
          @ApiResponse(responseCode = "400", description = "Requisição inválida"),
          @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
  })
  @PutMapping("/{id}")
  public Empresa atualizar(
          @Parameter(description = "ID da empresa", example = "1", required = true)
          @PathVariable Long id,

          @io.swagger.v3.oas.annotations.parameters.RequestBody(
                  description = "Dados para atualização",
                  required = true,
                  content = @Content(schema = @Schema(implementation = Empresa.class))
          )
          @RequestBody Empresa empresa
  ) {
    return empresaService.salvar(empresa.withId(id));
  }

  @Operation(
          summary = "Vincular/desvincular pessoas à empresa",
          description = """
          Vincula ou desvincula pessoas a uma empresa.

          - vinculo=true: vincula (padrão)
          - vinculo=false: desvincula

          O corpo da requisição deve conter uma lista de IDs de pessoas.
          """
  )
  @ApiResponses({
          @ApiResponse(responseCode = "204", description = "Operação realizada com sucesso"),
          @ApiResponse(responseCode = "400", description = "Requisição inválida"),
          @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
  })
  @PostMapping("/{empresaId}/pessoas")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void vincularOuDesvincularPessoas(
          @Parameter(description = "ID da empresa", example = "1", required = true)
          @PathVariable Long empresaId,

          @Parameter(description = "true para vincular, false para desvincular", example = "true")
          @RequestParam(defaultValue = "true") Boolean vinculo,

          @io.swagger.v3.oas.annotations.parameters.RequestBody(
                  description = "Lista de IDs de pessoas",
                  required = true,
                  content = @Content(
                          mediaType = MediaType.APPLICATION_JSON_VALUE,
                          array = @ArraySchema(schema = @Schema(implementation = Long.class)),
                          examples = @ExampleObject(
                                  name = "Exemplo",
                                  value = "[1, 2, 3]"
                          )
                  )
          )
          @RequestBody List<Long> pessoaIds
  ) {
    empresaService.vincularPessoas(empresaId, pessoaIds, vinculo);
  }
}
