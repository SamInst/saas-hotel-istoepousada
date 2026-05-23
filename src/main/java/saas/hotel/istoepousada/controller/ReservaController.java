package saas.hotel.istoepousada.controller;

import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Hospedagem;
import saas.hotel.istoepousada.dto.MotivoCancelamentoHospedagem;
import saas.hotel.istoepousada.service.HospedagemService;

@RestController
@RequestMapping("/reserva")
public class ReservaController {

  private final HospedagemService hospedagemService;

  public ReservaController(HospedagemService hospedagemService) {
    this.hospedagemService = hospedagemService;
  }

  @PostMapping("/{hospedagemId}/solicitar")
  public void solicitar(@PathVariable Long hospedagemId) {
    hospedagemService.solicitarReserva(hospedagemId);
  }

  @PutMapping("/{hospedagemId}/ativar")
  public void ativar(@PathVariable Long hospedagemId, @RequestBody Hospedagem.Request request) {
    hospedagemService.ativarReserva(hospedagemId, request);
  }

  @PutMapping("/{hospedagemId}/cancelar")
  public void cancelar(
      @PathVariable Long hospedagemId, @RequestBody MotivoCancelamentoHospedagem.Request motivo) {
    hospedagemService.cancelarReserva(hospedagemId, motivo);
  }

  @PutMapping("/{hospedagemId}/ausente")
  public void ausente(@PathVariable Long hospedagemId) {
    hospedagemService.marcarReservaAusente(hospedagemId);
  }
}
