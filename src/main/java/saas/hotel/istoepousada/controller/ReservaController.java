package saas.hotel.istoepousada.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

  /** Cancela uma reserva. */
  @PutMapping("/{id}/cancelar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelar(@PathVariable Long id) {
    reservaService.cancelar(id);
  }

  /** Ativa um orçamento (muda status de ORCAMENTO para ATIVO). */
  @PutMapping("/{id}/ativar")
  public Reserva ativar(@PathVariable Long id) {
    return reservaService.ativarOrcamento(id);
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

  /**
   * Calcula o preço de pernoite ou Day Use pelo quarto, período e datas de nascimento de cada
   * pessoa. Pessoas com 18+ são adultos; demais são crianças com suas idades calculadas.
   *
   * <p>Pernoite: informar data_entrada e data_saida.
   *
   * <p>Day Use: informar hora_inicio e hora_fim (LocalDateTime). Datas de entrada/saída opcionais.
   *
   * <p>Exemplos:
   *
   * <pre>
   * GET /reserva/calcular-preco?fk_quarto=1&data_entrada=20/04/2025&data_saida=23/04/2025
   *   &datas_nascimento=15/03/1990&datas_nascimento=10/06/2016
   *
   * GET /reserva/calcular-preco?fk_quarto=1
   *   &hora_inicio=20/04/2025 10:00&hora_fim=20/04/2025 18:00
   *   &datas_nascimento=15/03/1990&datas_nascimento=10/06/2016
   * </pre>
   */
  @GetMapping("/calcular-preco")
  public Reserva.ResultadoPreco calcularPrecoPorPessoas(
      @RequestParam Long fk_quarto,
      @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy")
          LocalDate data_entrada,
      @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data_saida,
      @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") List<LocalDate> datas_nascimento,
      @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
          LocalDateTime hora_inicio,
      @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
          LocalDateTime hora_fim) {
    return reservaService.calcularPrecoPorPessoas(
        fk_quarto, data_entrada, data_saida, datas_nascimento, hora_inicio, hora_fim);
  }
}
