package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.HistoricoRecebidosFuncionarioService;

@Tag(
        name = "Recebidos do Funcionário",
        description = "Endpoints de recebimentos vinculados ao histórico do funcionário.")
@RestController
@RequestMapping("/historico-recebidos-funcionario")
@RequireTela("ADMIN")
public class HistoricoRecebidosFuncionarioController {

    private final HistoricoRecebidosFuncionarioService historicoRecebidosFuncionarioService;

    public HistoricoRecebidosFuncionarioController(
            HistoricoRecebidosFuncionarioService historicoRecebidosFuncionarioService) {
        this.historicoRecebidosFuncionarioService = historicoRecebidosFuncionarioService;
    }


    @Operation(
            summary = "Buscar recebidos por histórico do funcionário",
            description = "Lista recebidos pelo id do histórico do funcionário.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista de recebidos")})
    @GetMapping
    public List<Funcionario.Historico.Recebido> buscar(
            @Parameter(description = "ID do histórico do funcionário", example = "1", required = true)
            @RequestParam
            Long historicoFuncionarioId) {
        return historicoRecebidosFuncionarioService.buscar(historicoFuncionarioId);
    }

    @Operation(
            summary = "Inserir recebido",
            description = "Insere um recebido com comprovante opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recebido criado"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Funcionario.Historico.Recebido inserir(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content =
                    @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            examples =
                            @ExampleObject(
                                    value =
                                            """
                                            {
                                              "recebido": {
                                                "historicoFuncionarioId": 1,
                                                "funcionarioId": 10,
                                                "tipoPagamentoId": 1,
                                                "nomePagador": "IstoePousada",
                                                "descricao": "Pagamento referente ao período da manhã",
                                                "valor": 1500.00,
                                                "dataHoraInicio": "2026-02-25T08:00:00",
                                                "dataHoraFim": "2026-02-25T12:00:00",
                                                "dataHoraPagamento": "2026-02-25T12:10:00"
                                              },
                                              "arquivo": "(binário)"
                                            }
                                            """)))
            @RequestPart("recebido")
            Funcionario.Historico.Recebido.Request recebido) throws IOException {
        return historicoRecebidosFuncionarioService.inserir(recebido);
    }

    @Operation(
            summary = "Atualizar recebido",
            description = "Atualiza um recebido com comprovante opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recebido atualizado"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "404", description = "Recebido não encontrado")
    })
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Funcionario.Historico.Recebido atualizar(
            Funcionario.Historico.Recebido.Update recebido)
            throws IOException {
        return historicoRecebidosFuncionarioService.atualizar(recebido);
    }
}
