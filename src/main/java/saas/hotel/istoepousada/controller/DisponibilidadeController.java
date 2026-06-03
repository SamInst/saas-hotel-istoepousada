package saas.hotel.istoepousada.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.service.HospedagemService;

@RestController
@RequestMapping("/disponibilidade")
public class DisponibilidadeController {

  private final HospedagemService hospedagemService;

  public DisponibilidadeController(HospedagemService hospedagemService) {
    this.hospedagemService = hospedagemService;
  }

  @GetMapping("/quartos")
  public List<Quarto.Disponibilidade> verificarDisponibilidadeQuartos(
      @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
      @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data_saida) {
    return hospedagemService.verificarDisponibilidadeQuartos(data_entrada, data_saida);
  }
}
