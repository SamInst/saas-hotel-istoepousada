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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saas.hotel.istoepousada.dto.Objeto;
import saas.hotel.istoepousada.repository.ObjectRepository;
import saas.hotel.istoepousada.security.RequireTela;

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
  @RequireTela("ADMIN")
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
  @RequireTela("ADMIN")
  public List<Objeto> permissoes(@RequestParam Long telaId) {
    return objectRepository.permissoes(telaId);
  }

  @Operation(
      summary = "Listar cargos do sistema",
      description =
          """
                    Retorna a lista de cargos cadastrados.
                    Normalmente usado no front para:
                    - cadastro/edição de funcionário
                    - filtros de listagem
                    - associação de cargo (telas/permissões) ao funcionário

                    Observação: este endpoint é apenas de leitura.
                    """)
  @ApiResponse(
      responseCode = "200",
      description = "Lista de cargos",
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
                                      { "id": 1, "descricao": "RECEPCAO" },
                                      { "id": 2, "descricao": "FINANCEIRO" },
                                      { "id": 3, "descricao": "GERENCIA" }
                                    ]
                                    """)))
  @GetMapping("/cargos")
  @RequireTela("ADMIN")
  public List<Objeto> cargos() {
    return objectRepository.cargos();
  }
}
