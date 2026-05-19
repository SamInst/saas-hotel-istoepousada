package saas.hotel.istoepousada.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Hospedagem;
import saas.hotel.istoepousada.dto.Item;
import saas.hotel.istoepousada.dto.MotivoCancelamentoHospedagem;
import saas.hotel.istoepousada.service.HospedagemService;

@RestController
@RequestMapping("/hospedagem")
public class HospedagemController {
  private final HospedagemService hospedagemService;

  public HospedagemController(HospedagemService hospedagemService) {
    this.hospedagemService = hospedagemService;
  }

  @PostMapping("/salvar")
  public void salvarHospedagem(@RequestBody List<Hospedagem.Request> hospedagens) {
    hospedagemService.salvarHospedagem(hospedagens);
  }

  @PostMapping("/{hospedagemId}/adicionar/diarias")
  public void adicionarDiarias(
      @PathVariable Long hospedagemId, @RequestBody List<Hospedagem.Diaria.Request> diarias) {
    hospedagemService.adicionarDiarias(hospedagemId, diarias);
  }

  /* Pagamento */
  @PostMapping("/{hospedagemId}/adicionar/pagamentos")
  public void adicionarPagamentos(
      @PathVariable Long hospedagemId, @RequestBody Hospedagem.Request request) {
    hospedagemService.adicionarPagamentos(hospedagemId, request);
  }

  @PutMapping("/editar/motivo-cancelamento")
  public void editarMotivoCancelamento(MotivoCancelamentoHospedagem.Request request) {
    hospedagemService.editarMotivoCancelamento(request);
  }

  /* Consumo */
  @PostMapping("/{hospedagemId}/adicionar/consumo")
  public void adicionarConsumo(
      @PathVariable Long hospedagemId, @RequestBody Item.Consumo.Request request) {
    hospedagemService.adicionarConsumo(hospedagemId, request);
  }

  @PutMapping("/editar/consumo")
  public void editarConsumo(@RequestBody Item.Consumo.Request request) {
    hospedagemService.editarConsumo(request);
  }
}
