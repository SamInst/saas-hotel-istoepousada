package saas.hotel.istoepousada.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Hospedagem;
import saas.hotel.istoepousada.dto.MotivoCancelamentoHospedagem;
import saas.hotel.istoepousada.service.HospedagemService;

@RestController
@RequestMapping("/dayuse")
public class DayUseController {

  private final HospedagemService hospedagemService;

  public DayUseController(HospedagemService hospedagemService) {
    this.hospedagemService = hospedagemService;
  }

  @PostMapping
  public void solicitar(@RequestBody List<Hospedagem.Request> requests) {
    requests.forEach(hospedagemService::solicitarDayUse);
  }

  @PutMapping("/{hospedagemId}/ativar")
  public void ativar(
      @PathVariable Long hospedagemId, @RequestBody Hospedagem.Request request) {
    hospedagemService.ativarDayUse(hospedagemId, request);
  }

  @PutMapping("/{hospedagemId}/cancelar")
  public void cancelar(
      @PathVariable Long hospedagemId,
      @RequestBody MotivoCancelamentoHospedagem.Request motivo) {
    hospedagemService.cancelarDayUse(hospedagemId, motivo);
  }

  @PutMapping("/{hospedagemId}/ausente")
  public void ausente(@PathVariable Long hospedagemId) {
    hospedagemService.marcarDayUseAusente(hospedagemId);
  }

  @PutMapping("/{hospedagemId}/finalizar")
  public void finalizar(@PathVariable Long hospedagemId) {
    hospedagemService.finalizarDayUse(hospedagemId);
  }

  @PutMapping("/{hospedagemId}/finalizar-pendente")
  public void finalizarPendente(@PathVariable Long hospedagemId) {
    hospedagemService.finalizarDayUsePagamentoPendente(hospedagemId);
  }
}
