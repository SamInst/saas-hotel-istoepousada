package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import saas.hotel.istoepousada.dto.ConfirmacaoPresenca;
import saas.hotel.istoepousada.security.PublicEndpoint;
import saas.hotel.istoepousada.service.ConfirmacaoPresencaService;

@Tag(
    name = "Chá de Bebê do Vicente",
    description = "Confirmação de presença (RSVP) pública para o Chá de Bebê do Vicente.")
@PublicEndpoint
@RestController
@RequestMapping("/cha-de-bebe")
public class ConfirmacaoPresencaController {

  private final ConfirmacaoPresencaService service;

  public ConfirmacaoPresencaController(ConfirmacaoPresencaService service) {
    this.service = service;
  }

  @Operation(summary = "Confirmar presença no Chá de Bebê do Vicente")
  @PostMapping("/confirmar")
  @ResponseStatus(HttpStatus.CREATED)
  public ConfirmacaoPresenca confirmar(@RequestBody ConfirmacaoPresenca.Request request) {
    return service.confirmar(request);
  }

  @Operation(summary = "Listar confirmações de presença (para os anfitriões)")
  @PublicEndpoint
  @GetMapping("/confirmacoes")
  public List<ConfirmacaoPresenca> listar() {
    return service.listar();
  }

  @Operation(summary = "Total de presenças confirmadas")
  @GetMapping("/total")
  public Map<String, Long> total() {
    return Map.of("total", service.total());
  }
}
