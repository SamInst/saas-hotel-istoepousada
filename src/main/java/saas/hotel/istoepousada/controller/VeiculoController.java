package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import saas.hotel.istoepousada.dto.Veiculo;
import saas.hotel.istoepousada.service.VeiculoService;

import java.util.List;

@Tag(
        name = "Veículos",
        description = "Cadastro de veículos e vínculo com pessoas."
)
@RestController
@RequestMapping("/veiculo")
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @Operation(
            summary = "Buscar veículo por ID",
            description = "Retorna os dados de um veículo pelo seu identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Veículo encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Veiculo.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Veiculo> buscarPorId(
            @Parameter(description = "ID do veículo", example = "1", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    @Operation(
            summary = "Listar todos os veículos",
            description = "Retorna todos os veículos cadastrados (sem paginação)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de veículos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Veiculo.class))
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<Veiculo>> listarTodos() {
        return ResponseEntity.ok(veiculoService.listarTodos());
    }

    @Operation(
            summary = "Listar veículos de uma pessoa",
            description = "Retorna os veículos vinculados a uma pessoa (inclui ativos e inativos)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de veículos vinculados",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Veiculo.class))
                    )
            )
    })
    @GetMapping("/pessoa/{pessoaId}")
    public ResponseEntity<List<Veiculo>> listarPorPessoa(
            @Parameter(description = "ID da pessoa", example = "10", required = true)
            @PathVariable Long pessoaId
    ) {
        return ResponseEntity.ok(veiculoService.listarPorPessoa(pessoaId));
    }

    @Operation(
            summary = "Criar veículo",
            description = "Cria um novo veículo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Veículo criado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Veiculo.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Requisição inválida")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Veiculo criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do veículo",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Veiculo.class),
                            examples = @ExampleObject(
                                    name = "Exemplo",
                                    value = """
                      {
                        "modelo": "Onix",
                        "marca": "Chevrolet",
                        "ano": 2022,
                        "placa": "ABC1D23",
                        "cor": "Branco"
                      }
                      """
                            )
                    )
            )
            @RequestBody Veiculo veiculo
    ) {
        return veiculoService.salvar(veiculo);
    }

    @Operation(
            summary = "Atualizar veículo",
            description = "Atualiza os dados de um veículo pelo ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Veículo atualizado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Veiculo.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    })
    @PutMapping("/{id}")
    public Veiculo atualizar(
            @Parameter(description = "ID do veículo", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody Veiculo veiculo
    ) {
        return veiculoService.salvar(veiculo.withId(id));
    }

    @Operation(
            summary = "Vincular veículo a uma pessoa",
            description = """
          Vincula um veículo a uma pessoa e marca o vínculo como ativo.
          Observação: por causa do UNIQUE(veiculo_id), um veículo só pode estar vinculado a uma pessoa por vez.
          Se já estiver em outra pessoa, o vínculo é movido para a pessoa informada.
          """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vínculo realizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    })
    @PostMapping("/{veiculoId}/pessoa/{pessoaId}/vincular")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vincular(
            @Parameter(description = "ID do veículo", example = "1", required = true)
            @PathVariable Long veiculoId,
            @Parameter(description = "ID da pessoa", example = "10", required = true)
            @PathVariable Long pessoaId
    ) {
        veiculoService.vincularPessoa(pessoaId, veiculoId);
    }

    @Operation(
            summary = "Ativar/desativar vínculo do veículo com a pessoa",
            description = """
          Define o status do vínculo (ativo/inativo).
          - ativo=true: ativa
          - ativo=false: desativa

          Observação: por causa do UNIQUE(veiculo_id), se o veículo estiver vinculado a outra pessoa,
          o vínculo será movido para a pessoa informada e então marcado conforme o parâmetro.
          """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status do vínculo atualizado"),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    })
    @PatchMapping("/{veiculoId}/pessoa/{pessoaId}/vinculo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setVinculoAtivo(
            @Parameter(description = "ID do veículo", example = "1", required = true)
            @PathVariable Long veiculoId,
            @Parameter(description = "ID da pessoa", example = "10", required = true)
            @PathVariable Long pessoaId,
            @Parameter(description = "true para ativar, false para desativar", example = "true", required = true)
            @RequestParam boolean ativo
    ) {
        veiculoService.setVinculoAtivo(pessoaId, veiculoId, ativo);
    }
}
