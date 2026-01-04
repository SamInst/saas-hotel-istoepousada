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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Notificacao;
import saas.hotel.istoepousada.service.NotificacaoService;

@Tag(name = "Notificações", description = "Consulta das notificações mais recentes.")
@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

  private final NotificacaoService notificacaoService;

  public NotificacaoController(NotificacaoService notificacaoService) {
    this.notificacaoService = notificacaoService;
  }

  @Operation(
      summary = "Listar últimas notificações por quantidade informada",
      description = "Retorna as notificações mais recentes, ordenadas por data/hora (DESC).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de notificações",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = Notificacao.class))))
  })
  @GetMapping
  public ResponseEntity<List<Notificacao>> listarUltimas20(@RequestParam Integer quantidade) {
    return ResponseEntity.ok(notificacaoService.listarUltimas20(quantidade));
  }

  @Operation(
      summary = "Listar últimas notificações por pessoa pela quantidade informada",
      description =
          "Retorna as notificações mais recentes de uma pessoa pela quantidade informada, ordenadas por data/hora (DESC).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de notificações da pessoa",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = Notificacao.class))))
  })
  @GetMapping("/pessoa/{pessoaId}")
  public ResponseEntity<List<Notificacao>> listarUltimas20PorPessoa(
      @Parameter(description = "ID da pessoa", example = "10", required = true) @PathVariable
          Long pessoaId,
      @RequestParam Integer quantidade) {
    return ResponseEntity.ok(notificacaoService.listarUltimas20PorPessoa(pessoaId, quantidade));
  }

  public record CriarNotificacaoRequest(Long fkPessoa, String nome, String descricao) {}

  @Operation(
      summary = "Criar notificação",
      description =
          "Cria uma notificação para uma pessoa. A data/hora é definida automaticamente (NOW()).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Notificação criada",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Notificacao.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
  })
  @PostMapping
  public ResponseEntity<Notificacao> criar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description =
                  "Dados para criação da notificação. Informe o id, nome e a mensagem a ser salva.",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = CriarNotificacaoRequest.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo",
                              value =
                                  """
                                  {
                                    "fkPessoa": 57,
                                    "nome": "Emerson Moraes",
                                    "descricao": "Cadastrou um novo usuário"
                                  }
                                  """)))
          @RequestBody
          CriarNotificacaoRequest request) {

    Notificacao criada =
        notificacaoService.criar(request.fkPessoa(), request.nome(), request.descricao());
    return ResponseEntity.ok(criada);
  }
}
