package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import saas.hotel.istoepousada.dto.HistoricoRecebidosFuncionario;
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

  public record RecebidoRequest(
      Long historicoFuncionarioId,
      Float valorRecebido,
      LocalDateTime dataHoraInicio,
      LocalDateTime dataHoraFim,
      LocalDateTime dataHoraPagamento,
      Long tipoPagamentoId,
      String descricao) {}

  @Operation(
      summary = "Buscar recebidos por histórico do funcionário",
      description = "Lista recebidos pelo id do histórico_funcionario.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista de recebidos")})
  @GetMapping
  public List<HistoricoRecebidosFuncionario> buscar(
      @Parameter(description = "ID do histórico_funcionario", example = "1", required = true)
          @RequestParam
          Long historicoFuncionarioId) {
    return historicoRecebidosFuncionarioService.buscar(historicoFuncionarioId);
  }

  @Operation(
      summary = "Inserir recebido (multipart)",
      description = "Insere um recebido com comprovante opcional.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Recebido criado"),
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
  })
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public HistoricoRecebidosFuncionario inserir(
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
                                                "valorRecebido": 1500.00,
                                                "dataHoraInicio": "2026-02-25T08:00:00",
                                                "dataHoraFim": "2026-02-25T12:00:00",
                                                "dataHoraPagamento": "2026-02-25T12:10:00",
                                                "tipoPagamentoId": 1,
                                                "descricao": "Pagamento referente ao período da manhã"
                                              },
                                              "arquivo": "(binário)"
                                            }
                                            """)))
          @RequestPart("recebido")
          RecebidoRequest recebido,
      @RequestPart(value = "arquivo", required = false) MultipartFile arquivo)
      throws IOException {

    HistoricoRecebidosFuncionario entity =
        new HistoricoRecebidosFuncionario(
            new HistoricoRecebidosFuncionario.HistoricoFuncionario(
                recebido.historicoFuncionarioId()),
            recebido.valorRecebido(),
            recebido.dataHoraInicio(),
            recebido.dataHoraFim(),
            recebido.dataHoraPagamento(),
            new HistoricoRecebidosFuncionario.TipoPagamento(recebido.tipoPagamentoId(), null),
            recebido.descricao(),
            null);

    return historicoRecebidosFuncionarioService.inserir(entity, arquivo);
  }

  @Operation(
      summary = "Atualizar recebido (multipart)",
      description = "Atualiza um recebido com comprovante opcional.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Recebido atualizado"),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Recebido não encontrado")
  })
  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public HistoricoRecebidosFuncionario atualizar(
      @Parameter(description = "ID do recebido", example = "1", required = true) @PathVariable
          Long id,
      @RequestPart("recebido") RecebidoRequest recebido,
      @RequestPart(value = "arquivo", required = false) MultipartFile arquivo)
      throws IOException {

    HistoricoRecebidosFuncionario entity =
        new HistoricoRecebidosFuncionario(
            id,
            new HistoricoRecebidosFuncionario.HistoricoFuncionario(
                recebido.historicoFuncionarioId()),
            recebido.valorRecebido(),
            recebido.dataHoraInicio(),
            recebido.dataHoraFim(),
            recebido.dataHoraPagamento(),
            new HistoricoRecebidosFuncionario.TipoPagamento(recebido.tipoPagamentoId(), null),
            recebido.descricao(),
            null);

    return historicoRecebidosFuncionarioService.atualizar(entity, arquivo);
  }
}
