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
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.QuartoService;

@Tag(name = "Quartos", description = "Endpoints de cadastro e consulta de quartos.")
@RestController
@RequestMapping("/quarto")
@RequireTela("CADASTRO")
public class QuartoController {

    private final QuartoService quartoService;

    public QuartoController(QuartoService quartoService) {
        this.quartoService = quartoService;
    }

    @Operation(
            summary = "Listar quartos (paginado) com filtros opcionais",
            description =
                    """
                    Lista quartos paginados. Filtros opcionais:
                    - id: busca específica por ID
                    - termo: busca global (descricao ILIKE e também id quando numérico)
                    - status: filtra pelo status do quarto
          
                    Se nenhum filtro for informado, retorna todos os quartos paginados.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de quartos",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    public Page<Quarto> listar(
            @Parameter(description = "ID do quarto") @RequestParam(required = false) Long id,
            @Parameter(description = "Busca global (descricao ILIKE ou id se numérico)")
            @RequestParam(required = false)
            String termo,
            @Parameter(
                    description =
                            "Status do quarto (OCUPADO, DISPONIVEL, RESERVADO, LIMPEZA, DIARIA_ENCERRADA, MANUTENCAO)")
            @RequestParam(required = false)
            Quarto.StatusQuarto status,
            @Parameter(description = "Número da página (0-based)", example = "0")
            @RequestParam(defaultValue = "0")
            int page,
            @Parameter(description = "Tamanho da página", example = "10")
            @RequestParam(defaultValue = "10")
            int size) {

        Pageable pageable = PageRequest.of(page, size);
        return quartoService.buscar(id, termo, status, pageable);
    }

    @Operation(
            summary = "Criar quarto",
            description = "Cria um novo quarto.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Quarto criado",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Quarto.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Quarto criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do quarto",
                    required = true,
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples =
                            @ExampleObject(
                                    name = "Exemplo",
                                    value =
                                            """
                                            {
                                              "descricao": "Quarto 101",
                                              "quantidade_pessoas": 2,
                                              "status_quarto": "DISPONIVEL",
                                              "qtd_cama_casal": 1,
                                              "qtd_cama_solteiro": 0,
                                              "qtd_rede": 0,
                                              "qtd_beliche": 0
                                            }
                                            """)))
            @RequestBody
            Quarto quarto) {
        return quartoService.criar(quarto);
    }

    @Operation(
            summary = "Atualizar quarto",
            description = "Atualiza os dados de um quarto existente.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Quarto atualizado",
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Quarto.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "404", description = "Quarto não encontrado")
    })
    @PutMapping("/{id}")
    public Quarto atualizar(
            @Parameter(description = "ID do quarto", required = true, example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do quarto",
                    required = true,
                    content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples =
                            @ExampleObject(
                                    name = "Exemplo",
                                    value =
                                            """
                                            {
                                              "descricao": "Quarto 101 (Reformado)",
                                              "quantidade_pessoas": 3,
                                              "status_quarto": "RESERVADO",
                                              "qtd_cama_casal": 1,
                                              "qtd_cama_solteiro": 1,
                                              "qtd_rede": 0,
                                              "qtd_beliche": 0
                                            }
                                            """)))
            @RequestBody
            Quarto quarto) {
        return quartoService.atualizar(id, quarto);
    }

    @Operation(
            summary = "Remover quarto",
            description = "Remove um quarto pelo ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Quarto removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Quarto não encontrado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(
            @Parameter(description = "ID do quarto", required = true, example = "1") @PathVariable Long id) {
        quartoService.remover(id);
    }
}
