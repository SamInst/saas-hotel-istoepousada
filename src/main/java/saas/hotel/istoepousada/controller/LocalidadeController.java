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
import saas.hotel.istoepousada.dto.Objeto;
import saas.hotel.istoepousada.service.EnderecoService;

@Tag(
    name = "Localidade (Endereço)",
    description = "Endpoints utilitários para preencher combobox de País, Estado e Município.")
@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class LocalidadeController {

  private final EnderecoService enderecoService;

  public LocalidadeController(EnderecoService enderecoService) {
    this.enderecoService = enderecoService;
  }

  @Operation(
      summary = "Listar países",
      description = "Retorna todos os países cadastrados para preencher o combobox de país.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de países",
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
                        { "id": 1, "descricao": "Brasil" },
                        { "id": 2, "descricao": "Portugal" }
                      ]
                      """)))
  })
  @GetMapping("/paises")
  public ResponseEntity<List<Objeto>> listarPaises() {
    List<Objeto> response = enderecoService.listarPaises();
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Listar estados por país",
      description =
          "Retorna os estados vinculados a um país (fkPais) para preencher o combobox de estado.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de estados do país informado",
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
                        { "id": 10, "descricao": "Maranhão" },
                        { "id": 11, "descricao": "Piauí" }
                      ]
                      """))),
    @ApiResponse(responseCode = "400", description = "Parâmetro inválido")
  })
  @GetMapping("/estados/{fkPais}")
  public ResponseEntity<List<Objeto>> listarEstadosPorPais(
      @Parameter(description = "ID do país (fk_pais)", example = "1", required = true) @PathVariable
          Long fkPais) {
    List<Objeto> response = enderecoService.listarEstados(fkPais);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Listar municípios por estado",
      description =
          "Retorna os municípios vinculados a um estado (fkEstado) para preencher o combobox de município.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de municípios do estado informado",
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
                        { "id": 100, "descricao": "São Luís" },
                        { "id": 101, "descricao": "Imperatriz" }
                      ]
                      """))),
    @ApiResponse(responseCode = "400", description = "Parâmetro inválido")
  })
  @GetMapping("/municipios/{fkEstado}")
  public ResponseEntity<List<Objeto>> listarMunicipiosPorEstado(
      @Parameter(description = "ID do estado (fk_estado)", example = "10", required = true)
          @PathVariable
          Long fkEstado) {
    List<Objeto> response = enderecoService.listarMunicipios(fkEstado);
    return ResponseEntity.ok(response);
  }
}
