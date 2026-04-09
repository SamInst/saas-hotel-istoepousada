package saas.hotel.istoepousada.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import saas.hotel.istoepousada.dto.Reserva;
import saas.hotel.istoepousada.service.ReservaService;

@RestController
@RequestMapping("/reserva")
public class ReservaController {

  private final ReservaService reservaService;

  public ReservaController(ReservaService reservaService) {
    this.reservaService = reservaService;
  }

  /**
   * Busca reservas por mês/ano, agrupadas por dia. Filtros opcionais: id da reserva e nome da
   * pessoa.
   */
  @GetMapping
  public List<Reserva.PorDia> buscarPorMesAno(
      @RequestParam int mes,
      @RequestParam int ano,
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String nome) {
    return reservaService.buscarPorMesAno(mes, ano, id, nome);
  }

  /**
   * Busca todas as reservas de uma data específica. Filtros opcionais: id da reserva e nome da
   * pessoa.
   */
  @GetMapping("/data")
  public List<Reserva> buscarPorData(
      @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data,
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String nome) {
    return reservaService.buscarPorData(data, id, nome);
  }

  /** Busca todas as reservas do mês para um quarto específico. */
  @GetMapping("/quarto/{quartoId}/mes")
  public List<Reserva> buscarPorQuartoMes(
      @PathVariable Long quartoId, @RequestParam int mes, @RequestParam int ano) {
    return reservaService.buscarPorQuartoMes(quartoId, mes, ano);
  }

  /** Retorna uma reserva por id. */
  @GetMapping("/{id}")
  public Reserva findById(@PathVariable Long id) {
    return reservaService.findById(id);
  }

  /** Insere múltiplas reservas de uma vez, incluindo pessoas e pagamentos. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public List<Reserva> inserir(@RequestBody @Valid Reserva.BatchRequest request) {
    return reservaService.inserirBatch(request);
  }

  /** Edita uma reserva (apenas quarto e/ou datas). */
  @PutMapping
  public Reserva atualizar(@RequestBody @Valid Reserva.Update update) {
    return reservaService.atualizar(update);
  }

  /** Cancela uma reserva. */
  @PutMapping("/{id}/cancelar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelar(@PathVariable Long id) {
    reservaService.cancelar(id);
  }

  /**
   * Calcula o preço de uma ou mais reservas com base no quarto, período, adultos e crianças. As
   * regras de preço são buscadas da categoria do quarto (com suporte a sazonalidade).
   */
  @PostMapping("/calcular-preco")
  public List<Reserva.ResultadoPreco> calcularPrecos(
      @RequestBody @Valid List<Reserva.CalculoPrecosRequest> requests) {
    return reservaService.calcularPrecos(requests);
  }
}
