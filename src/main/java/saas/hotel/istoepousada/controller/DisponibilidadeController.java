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

/**
 * Endpoints de disponibilidade de quartos.
 *
 * <p>Exemplo de uso:
 * <pre>
 * GET /disponibilidade/quartos?data_entrada=26/05/2026&data_saida=28/05/2026
 * </pre>
 */
@RestController
@RequestMapping("/disponibilidade")
public class DisponibilidadeController {

  private final HospedagemService hospedagemService;

  public DisponibilidadeController(HospedagemService hospedagemService) {
    this.hospedagemService = hospedagemService;
  }

  /**
   * Retorna todos os quartos com o flag {@code disponivel} para o período solicitado.
   *
   * @param data_entrada data de entrada no formato dd/MM/yyyy
   * @param data_saida   data de saída   no formato dd/MM/yyyy
   * @return lista com id, descricao e disponibilidade de cada quarto
   */
  @GetMapping("/quartos")
  public List<Quarto.Disponibilidade> verificarDisponibilidadeQuartos(
      @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
      @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data_saida) {
    return hospedagemService.verificarDisponibilidadeQuartos(data_entrada, data_saida);
  }
}
