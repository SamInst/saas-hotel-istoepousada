//package saas.hotel.istoepousada.controller;
//
//import jakarta.validation.Valid;
//import java.time.LocalDate;
//import java.util.List;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.ResponseStatus;
//import org.springframework.web.bind.annotation.RestController;
//import saas.hotel.istoepousada.dto.Reserva;
//import saas.hotel.istoepousada.service.ReservaService;
//
//@RestController
//@RequestMapping("/reserva")
//public class ReservaController {
//
//  private final ReservaService reservaService;
//
//  public ReservaController(ReservaService reservaService) {
//    this.reservaService = reservaService;
//  }
//
//  /**
//   * Busca reservas por mês/ano, agrupadas por dia. Filtros opcionais: id da reserva e nome da
//   * pessoa.
//   */
//  @GetMapping
//  public List<Reserva.PorDia> buscarReservas(
//      @RequestParam(required = false) Integer mes,
//      @RequestParam(required = false) Integer ano,
//      @RequestParam(required = false) Long id,
//      @RequestParam(required = false) String nome,
//      @RequestParam(required = false) List<Reserva.Status> status) {
//    if (mes != null && ano != null) {
//      return reservaService.buscarPorMesAno(mes, ano, id, nome, status);
//    }
//    if ((nome != null && !nome.isBlank()) || (status != null && !status.isEmpty())) {
//      return reservaService.buscarPorFiltro(nome, status);
//    }
//    throw new IllegalArgumentException("Informe mes e ano, ou nome/status para busca.");
//  }
//
//  /**
//   * Busca todas as reservas de uma data específica. Filtros opcionais: id da reserva e nome da
//   * pessoa.
//   */
//  @GetMapping("/data")
//  public List<Reserva> buscarPorData(
//      @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data,
//      @RequestParam(required = false) Long id,
//      @RequestParam(required = false) String nome) {
//    return reservaService.buscarPorData(data, id, nome);
//  }
//
//  /** Busca todas as reservas do mês para um quarto específico. */
//  @GetMapping("/quarto/{quartoId}/mes")
//  public List<Reserva> buscarPorQuartoMes(
//      @PathVariable Long quartoId, @RequestParam int mes, @RequestParam int ano) {
//    return reservaService.buscarPorQuartoMes(quartoId, mes, ano);
//  }
//
//  /** Retorna uma reserva por id. */
//  @GetMapping("/{id:\\d+}")
//  public Reserva findById(@PathVariable Long id) {
//    return reservaService.findById(id);
//  }
//
////  /** Insere múltiplas reservas de uma vez, incluindo pessoas e pagamentos. */
////  @PostMapping
////  @ResponseStatus(HttpStatus.CREATED)
////  public List<Reserva> inserir(@RequestBody @Valid Reserva.BatchRequest request) {
////    return reservaService.inserirBatch(request);
////  }
//
//  /** Edita uma reserva (apenas quarto e/ou datas). */
//  @PutMapping
//  public Reserva atualizar(@RequestBody @Valid Reserva.Update update) {
//    return reservaService.atualizar(update);
//  }
//
//  /** Vincula ou desvincula uma pessoa da reserva conforme o campo `vincular`. */
//  @PostMapping("/{id}/pessoa/{pessoaId}")
//  public List<Reserva.ReservaPessoa> togglePessoa(
//      @PathVariable Long id,
//      @PathVariable Long pessoaId,
//      @RequestBody @Valid Reserva.PessoaToggleRequest request) {
//    return reservaService.togglePessoa(id, pessoaId, request);
//  }
//
//  /** Adiciona um pagamento a uma reserva existente. */
//  @PostMapping("/{id}/pagamento")
//  @ResponseStatus(HttpStatus.CREATED)
//  public Reserva adicionarPagamento(
//      @PathVariable Long id, @RequestBody @Valid Reserva.PagamentoReservaRequest request) {
//    return reservaService.adicionarPagamento(id, request);
//  }
//
//  /** Retorna um orçamento com suas reservas vinculadas. */
//  @GetMapping("/orcamento/{orcamentoId}")
//  public Reserva.OrcamentoDetalhe buscarPorOrcamento(@PathVariable Long orcamentoId) {
//    return reservaService.buscarPorOrcamento(orcamentoId);
//  }
//
//  /** Atualiza o status de uma lista de reservas. */
//  @PutMapping("/status")
//  @ResponseStatus(HttpStatus.NO_CONTENT)
//  public void atualizarStatus(@RequestBody Reserva.AtualizarStatusRequest request) {
//    reservaService.atualizarStatus(request);
//  }
//
//  /** Verifica disponibilidade de uma lista de quartos em um período. */
//  @GetMapping("/disponibilidade")
//  public List<Reserva.Disponibilidade> verificarDisponibilidade(
//      @RequestParam List<Long> fk_quartos,
//      @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
//      @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate data_saida) {
//    return reservaService.verificarDisponibilidade(fk_quartos, data_entrada, data_saida);
//  }
//
//  /** Cancela uma reserva registrando o motivo. Retorna o motivo de cancelamento registrado. */
//  @PutMapping("/{id}/cancelar")
//  @ResponseStatus(HttpStatus.CREATED)
//  public Reserva.MotivoCancelamento cancelar(
//      @PathVariable Long id, @RequestBody @Valid Reserva.CancelamentoRequest request) {
//    return reservaService.cancelarComMotivo(id, request.motivo_cancelamento());
//  }
//
//
//}
