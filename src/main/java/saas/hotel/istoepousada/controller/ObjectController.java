package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.hotel.istoepousada.dto.Objeto;
import saas.hotel.istoepousada.repository.ObjectRepository;

@Tag(
    name = "Enums (Combos)",
    description =
        "Endpoints utilitários para listar opções usadas em combobox (ex.: tipo de pagamento, telas e permissões).")
@RestController
@RequestMapping("/enum")
public class ObjectController {

  private final ObjectRepository objectRepository;

  public ObjectController(ObjectRepository objectRepository) {
    this.objectRepository = objectRepository;
  }

  @Operation(
      summary = "Listar tipos de pagamento",
      description =
          """
                  Retorna a lista de tipos de pagamento cadastrados no banco, usada para preencher combobox
                  no cadastro de reservas, relatórios financeiros, contas, etc.

                  Observação: este endpoint é apenas de leitura.
                  """)
  @ApiResponse(
      responseCode = "200",
      description = "Lista de tipos de pagamento",
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
                                    { "id": 1, "descricao": "DINHEIRO" },
                                    { "id": 2, "descricao": "PIX" },
                                    { "id": 3, "descricao": "CARTAO_CREDITO" }
                                  ]
                                  """)))
  @GetMapping("/tipo-pagamento")
  public List<Objeto> tipoPagamentoEnum() {
    return objectRepository.tipoPagamento();
  }
}
