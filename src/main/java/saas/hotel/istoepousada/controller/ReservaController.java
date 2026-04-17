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

  /** Adiciona uma pessoa a uma reserva existente. */
  @PostMapping("/{id}/pessoa")
  @ResponseStatus(HttpStatus.CREATED)
  public Reserva adicionarPessoa(
      @PathVariable Long id, @RequestBody @Valid Reserva.PessoaRequest request) {
    return reservaService.adicionarPessoa(id, request);
  }

  /** Adiciona um pagamento a uma reserva existente. */
  @PostMapping("/{id}/pagamento")
  @ResponseStatus(HttpStatus.CREATED)
  public Reserva adicionarPagamento(
      @PathVariable Long id, @RequestBody @Valid Reserva.PagamentoReservaRequest request) {
    return reservaService.adicionarPagamento(id, request);
  }

  /** Retorna todas as reservas vinculadas a um orçamento. */
  @GetMapping("/orcamento/{orcamentoId}")
  public List<Reserva> buscarPorOrcamento(@PathVariable Long orcamentoId) {
    return reservaService.buscarPorOrcamento(orcamentoId);
  }

  /** Atualiza o status de uma lista de reservas. */
  @PutMapping("/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void atualizarStatus(@RequestBody Reserva.AtualizarStatusRequest request) {
    reservaService.atualizarStatus(request);
  }

  /** Calcula o preço para um ou mais quartos. Cada item tem seu próprio quarto, datas e pessoas. */
  @PostMapping("/calcular-preco")
  public List<Reserva.ResultadoPreco> calcularPreco(
      @RequestBody List<Reserva.CalcularPrecoRequest> requests) {
    return reservaService.calcularPreco(requests);
  }
}
