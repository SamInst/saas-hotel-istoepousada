// package saas.hotel.istoepousada.service;
//
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.time.LocalTime;
// import java.time.temporal.ChronoUnit;
// import java.util.ArrayList;
// import java.util.Comparator;
// import java.util.List;
// import java.util.Map;
// import java.util.UUID;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import saas.hotel.istoepousada.dto.*;
// import saas.hotel.istoepousada.handler.exceptions.BusinessException;
// import saas.hotel.istoepousada.handler.exceptions.ConflictException;
// import saas.hotel.istoepousada.repository.CategoriaRepository;
// import saas.hotel.istoepousada.repository.PagamentoRepository;
// import saas.hotel.istoepousada.repository.PernoiteRepository;
// import saas.hotel.istoepousada.repository.QuartoRepository;
// import saas.hotel.istoepousada.repository.RelatorioRepository;
//
// @Service
// public class PernoiteService {
//
//  private final PernoiteRepository pernoiteRepository;
//  private final ReservaRepository reservaRepository;
//  private final CategoriaRepository categoriaRepository;
//  private final PagamentoRepository pagamentoRepository;
//  private final RelatorioRepository relatorioRepository;
//  private final QuartoRepository quartoRepository;
//
//  public PernoiteService(
//      PernoiteRepository pernoiteRepository,
//      ReservaRepository reservaRepository,
//      CategoriaRepository categoriaRepository,
//      PagamentoRepository pagamentoRepository,
//      RelatorioRepository relatorioRepository,
//      QuartoRepository quartoRepository) {
//    this.pernoiteRepository = pernoiteRepository;
//    this.reservaRepository = reservaRepository;
//    this.categoriaRepository = categoriaRepository;
//    this.pagamentoRepository = pagamentoRepository;
//    this.relatorioRepository = relatorioRepository;
//    this.quartoRepository = quartoRepository;
//  }
//
//  // ── Criação ─────────────────────────────────────────────────────────────────
//
////  @Transactional
////  public Pernoite create(Pernoite.CreateRequest request) {
////    if (request.reserva_id() != null) {
////      validarCamposNulosParaReserva(request);
////      return criarDeReserva(request.reserva_id());
////    }
////    validarCamposObrigatoriosSemReserva(request);
////    return criarSemReserva(request);
////  }
//
//  private void validarCamposNulosParaReserva(Pernoite.CreateRequest request) {
//    if (request.quarto_id() != null
//        || request.data_entrada() != null
//        || request.data_saida() != null
//        || (request.pessoas() != null && !request.pessoas().isEmpty())
//        || (request.pagamentos() != null && !request.pagamentos().isEmpty())) {
//      throw new BusinessException(
//          "Quando reserva_id é informado, os demais campos devem ser nulos.");
//    }
//  }
//
//  private void validarCamposObrigatoriosSemReserva(Pernoite.CreateRequest request) {
//    if (request.quarto_id() == null) throw new IllegalArgumentException("quarto_id é
// obrigatório.");
//    if (request.data_entrada() == null)
//      throw new IllegalArgumentException("data_entrada é obrigatória.");
//    if (request.data_saida() == null)
//      throw new IllegalArgumentException("data_saida é obrigatória.");
//    if (!request.data_entrada().isBefore(request.data_saida()))
//      throw new IllegalArgumentException("data_saida deve ser posterior a data_entrada.");
//    if (request.pessoas() == null || request.pessoas().isEmpty())
//      throw new IllegalArgumentException("pessoas é obrigatório.");
//  }
//
////  private Pernoite criarDeReserva(Long reservaId) {
////    Reserva reserva = reservaRepository.findById(reservaId);
////    if (reserva.quarto() == null)
////      throw new BusinessException("Reserva não possui quarto vinculado.");
////
////    Long quartoId = reserva.quarto().id();
////    LocalDate dataEntrada = reserva.data_hora_entrada().toLocalDate();
////    LocalDate dataSaida = reserva.data_hora_saida().toLocalDate();
////    List<Long> pessoaIds =
////        reserva.pessoas() == null
////            ? List.of()
////            : reserva.pessoas().stream().map(rp -> rp.pessoa().id()).toList();
////
////    ReservaRepository.CategoriaCheckin catInfo =
////        reservaRepository.findCategoriaCheckinByQuartoId(quartoId);
////    if (catInfo == null)
////      throw new BusinessException("Quarto " + quartoId + " não possui categoria configurada.");
////
////    LocalDateTime entrada = buildDateTime(dataEntrada, catInfo.hora_checkin());
////    LocalDateTime saida = buildDateTime(dataSaida, catInfo.hora_checkout());
////
////    validarDisponibilidade(quartoId, entrada, saida, null, reservaId);
////
////    int noites = (int) ChronoUnit.DAYS.between(dataEntrada, dataSaida);
////    if (noites <= 0) noites = 1;
////
////    Float valorTotal =
////        reserva.valor_total() != null
////            ? reserva.valor_total()
////            : calcularValorTotal(catInfo, dataEntrada, noites, pessoaIds.size());
////
////    reservaRepository.atualizarStatus(List.of(reservaId), Reserva.Status.HOSPEDADO);
////
////    Long pernoiteId = pernoiteRepository.insertPernoite(dataEntrada, dataSaida, valorTotal);
////
////    List<Long> diariaIds =
////        criarDiarias(pernoiteId, catInfo, dataEntrada, noites, pessoaIds.size(), quartoId);
////
////    quartoRepository.updateStatus(quartoId, Quarto.Status.OCUPADO);
////
////    if (!pessoaIds.isEmpty() && !diariaIds.isEmpty()) {
////      pernoiteRepository.addPessoasToDiaria(diariaIds.getFirst(), pessoaIds);
////    }
////
////    if (reserva.pagamentos() != null && !reserva.pagamentos().isEmpty() && !diariaIds.isEmpty())
// {
////      for (Reserva.ReservaPagamento rp : reserva.pagamentos()) {
////        if (rp.pagamento() != null && rp.pagamento().uuid() != null) {
////          pernoiteRepository.addPagamentoToPernoite(pernoiteId, rp.pagamento().uuid());
////        }
////      }
////    }
////
////    return pernoiteRepository.findById(pernoiteId);
////  }
//
////  private Pernoite criarSemReserva(Pernoite.CreateRequest request) {
////    ReservaRepository.CategoriaCheckin catInfo =
////        reservaRepository.findCategoriaCheckinByQuartoId(request.quarto_id());
////    if (catInfo == null)
////      throw new BusinessException(
////          "Quarto " + request.quarto_id() + " não possui categoria configurada.");
////
////    LocalDateTime entrada = buildDateTime(request.data_entrada(), catInfo.hora_checkin());
////    LocalDateTime saida = buildDateTime(request.data_saida(), catInfo.hora_checkout());
////
////    validarDisponibilidade(request.quarto_id(), entrada, saida, null, null);
////
////    int noites = (int) ChronoUnit.DAYS.between(request.data_entrada(), request.data_saida());
////    if (noites <= 0) noites = 1;
////
////    int qtdPessoas = request.pessoas().size();
////    float valorTotal = calcularValorTotal(catInfo, request.data_entrada(), noites, qtdPessoas);
////
////    Long reservaId =
////        reservaRepository.inserirHospedado(request.quarto_id(), entrada, saida, valorTotal);
////    for (Long pessoaId : request.pessoas()) {
////      reservaRepository.vincularPessoa(reservaId, pessoaId, false);
////    }
////
////    Long pernoiteId =
////        pernoiteRepository.insertPernoite(request.data_entrada(), request.data_saida(),
// valorTotal);
////
////    List<Long> diariaIds =
////        criarDiarias(
////            pernoiteId, catInfo, request.data_entrada(), noites, qtdPessoas,
// request.quarto_id());
////
////    quartoRepository.updateStatus(request.quarto_id(), Quarto.Status.OCUPADO);
////
////    if (!diariaIds.isEmpty()) {
////      pernoiteRepository.addPessoasToDiaria(diariaIds.getFirst(), request.pessoas());
////      if (request.pagamentos() != null && !request.pagamentos().isEmpty()) {
////        for (Pagamento.Request pg : request.pagamentos()) {
////          Pagamento criado = pagamentoRepository.create(pg);
////          pernoiteRepository.addPagamentoToPernoite(pernoiteId, criado.uuid());
////          relatorioRepository.registrarRelatorioDeConsumo(
////                  criado,
////                  relatorioRepository.getFuncionarioId(),
////                  "Pernoite - Quarto " + request.quarto_id());
////        }
////      }
////    }
////
////    return pernoiteRepository.findById(pernoiteId);
////  }
//
////  private List<Long> criarDiarias(
////      Long pernoiteId,
////      ReservaRepository.CategoriaCheckin catInfo,
////      LocalDate dataEntrada,
////      int noites,
////      int qtdPessoas,
////      Long quartoId) {
////    List<ReservaRepository.Sazonalidade> sazonalidades =
////        reservaRepository.findSazonalidades(catInfo.id());
////
////    List<Long> sazonIds =
// sazonalidades.stream().map(ReservaRepository.Sazonalidade::id).toList();
////    Map<Long, List<Categoria.ModeloOcupacao>> modelosOcupacao =
////        sazonIds.isEmpty() ? Map.of() : reservaRepository.findSazonModelosOcupacao(sazonIds);
////    Map<Long, List<Categoria.ModeloFixo>> modelosFixo =
////        sazonIds.isEmpty() ? Map.of() : reservaRepository.findSazonModelosFixo(sazonIds);
////
////    Categoria categoria =
////        categoriaRepository
////            .findCategoriasParaCalculo(List.of(catInfo.id()))
////            .getOrDefault(catInfo.id(), null);
////
////    List<Long> diariaIds = new ArrayList<>();
////    for (int i = 0; i < noites; i++) {
////      LocalDate noite = dataEntrada.plusDays(i);
////      LocalDateTime inicio = buildDateTime(noite, catInfo.hora_checkin());
////      LocalDateTime fim = buildDateTime(noite.plusDays(1), catInfo.hora_checkout());
////
////      float valor =
////          categoria != null
////              ? (float)
////                  calcularPrecoNoite(
////                      noite, categoria, sazonalidades, modelosOcupacao, modelosFixo, qtdPessoas)
////              : 0f;
////
////      Long diariaId =
////          pernoiteRepository.insertDiaria(pernoiteId, i + 1, inicio, fim, valor, quartoId);
////      diariaIds.add(diariaId);
////    }
////    return diariaIds;
////  }
//
//  // ── Edição ──────────────────────────────────────────────────────────────────
//
////  @Transactional
////  public Pernoite edit(Long id, Pernoite.EditRequest request) {
////    Pernoite existing = pernoiteRepository.findById(id);
////
////    Long quartoId =
////        request.quarto_id() != null
////            ? request.quarto_id()
////            : pernoiteRepository.getQuartoIdByPernoite(id);
////
////    ReservaRepository.CategoriaCheckin catInfo =
////        reservaRepository.findCategoriaCheckinByQuartoId(quartoId);
////    if (catInfo == null)
////      throw new BusinessException("Quarto " + quartoId + " não possui categoria configurada.");
////
////    LocalDate dataEntrada =
////        request.data_entrada() != null ? request.data_entrada() : existing.check_in();
////    LocalDate dataSaida =
////        request.data_saida() != null ? request.data_saida() : existing.check_out();
////
////    if (!dataEntrada.isBefore(dataSaida))
////      throw new BusinessException("data_saida deve ser posterior a data_entrada.");
////
////    LocalDateTime novaEntrada = buildDateTime(dataEntrada, catInfo.hora_checkin());
////    LocalDateTime novaSaida = buildDateTime(dataSaida, catInfo.hora_checkout());
////
////    validarDisponibilidade(quartoId, novaEntrada, novaSaida, id, null);
////
////    int novosNoites = (int) ChronoUnit.DAYS.between(dataEntrada, dataSaida);
////    if (novosNoites <= 0) novosNoites = 1;
////    int atuaisNoites = existing.quantidade_diarias();
////
////    List<ReservaRepository.Sazonalidade> sazonalidades =
////        reservaRepository.findSazonalidades(catInfo.id());
////    List<Long> sazonIds =
// sazonalidades.stream().map(ReservaRepository.Sazonalidade::id).toList();
////    Map<Long, List<Categoria.ModeloOcupacao>> modelosOcupacao =
////        sazonIds.isEmpty() ? Map.of() : reservaRepository.findSazonModelosOcupacao(sazonIds);
////    Map<Long, List<Categoria.ModeloFixo>> modelosFixo =
////        sazonIds.isEmpty() ? Map.of() : reservaRepository.findSazonModelosFixo(sazonIds);
////    Categoria categoria =
////        categoriaRepository
////            .findCategoriasParaCalculo(List.of(catInfo.id()))
////            .getOrDefault(catInfo.id(), null);
////
////    int qtdPessoas = existing.pessoas().size();
////    if (qtdPessoas == 0) qtdPessoas = 1;
////
////    if (novosNoites < atuaisNoites) {
////      pernoiteRepository.deleteDiariasAPartirDoNumero(id, novosNoites + 1);
////    } else if (novosNoites > atuaisNoites) {
////      for (int i = atuaisNoites; i < novosNoites; i++) {
////        LocalDate noite = dataEntrada.plusDays(i);
////        LocalDateTime inicio = buildDateTime(noite, catInfo.hora_checkin());
////        LocalDateTime fim = buildDateTime(noite.plusDays(1), catInfo.hora_checkout());
////        float valor =
////            categoria != null
////                ? (float)
////                    calcularPrecoNoite(
////                        noite, categoria, sazonalidades, modelosOcupacao, modelosFixo,
// qtdPessoas)
////                : 0f;
////        pernoiteRepository.insertDiaria(id, i + 1, inicio, fim, valor, quartoId);
////      }
////    }
////
////    List<Pernoite.Diaria> diariasAtuais = pernoiteRepository.findDiariasByPernoite(id);
////
////    if (request.quarto_id() != null) {
////      for (Pernoite.Diaria d : diariasAtuais) {
////        pernoiteRepository.updateDiariaQuarto(d.id(), quartoId);
////      }
////    }
////
////    float novoValorTotal = 0f;
////    for (int i = 0; i < diariasAtuais.size(); i++) {
////      LocalDate noite = dataEntrada.plusDays(i);
////      if (categoria != null) {
////        float novoValor =
////            (float)
////                calcularPrecoNoite(
////                    noite, categoria, sazonalidades, modelosOcupacao, modelosFixo, qtdPessoas);
////        pernoiteRepository.updateDiariaValor(diariasAtuais.get(i).id(), novoValor);
////        novoValorTotal += novoValor;
////      } else {
////        novoValorTotal += diariasAtuais.get(i).valor();
////      }
////    }
////
////    pernoiteRepository.updatePernoite(
////        id, dataEntrada, dataSaida, request.hora_chegada(), request.hora_saida(),
// novoValorTotal);
////
////
////
////    return pernoiteRepository.findById(id);
////  }
//
//  // ── Alterar status ──────────────────────────────────────────────────────────
//
////  @Transactional
////  public Pernoite updateStatus(Long id, Pernoite.Status status) {
////    Pernoite pernoite = pernoiteRepository.findById(id);
////    pernoiteRepository.updateStatus(id, status);
////    if (status == Pernoite.Status.FINALIZADO
////        || status == Pernoite.Status.FINALIZADO_PAGAMENTO_PENDENTE) {
////      try {
////        Long quartoId = pernoiteRepository.getQuartoIdByPernoite(id);
////        quartoRepository.updateStatus(quartoId, Quarto.Status.LIMPEZA);
////
////        var reserva_id = reservaRepository.findByDatasEQuarto(
////                pernoite.diarias().getLast().quarto().id(),
////                pernoite.check_in(),
////                pernoite.check_out()
////        );
////
////        reservaRepository.atualizarStatus(List.of(reserva_id), Reserva.Status.FINALIZADO);
////
////      } catch (Exception ignored) {
////      }
////    } else if (status == Pernoite.Status.CANCELADO) {
////      try {
////        Long quartoId = pernoiteRepository.getQuartoIdByPernoite(id);
////        quartoRepository.updateStatus(quartoId, Quarto.Status.DISPONIVEL);
////      } catch (Exception ignored) {
////      }
////    }
////    return pernoiteRepository.findById(id);
////  }
//
//  @Transactional
//  public Pernoite.Diaria updateDiariaStatus(Long diariaId, Pernoite.Diaria.Status status) {
//    pernoiteRepository.getPernoiteIdByDiaria(diariaId);
//    pernoiteRepository.updateDiariaStatus(diariaId, status);
//    return pernoiteRepository.findDiariaById(diariaId);
//  }
//
//  // ── Diária – quarto ─────────────────────────────────────────────────────────
//
//  @Transactional
//  public Pernoite.Diaria updateDiariaQuarto(Long diariaId, Long novoQuartoId) {
//    Pernoite.Diaria diaria = pernoiteRepository.findDiariaById(diariaId);
//
//    Long quartoAnteriorId = diaria.quarto() != null ? diaria.quarto().id() : null;
//
//    if (novoQuartoId.equals(quartoAnteriorId)) {
//      return diaria;
//    }
//
//    Long pernoiteId = pernoiteRepository.getPernoiteIdByDiaria(diariaId);
//
//    if (pernoiteRepository.hasConflito(
//        novoQuartoId,
//        diaria.data_hora_inicio(),
//        diaria.data_hora_fim(),
//        pernoiteId,
//        null)) {
//      throw new ConflictException(
//          "Quarto " + novoQuartoId + " possui ocupação conflitante no período informado.");
//    }
//
//    pernoiteRepository.updateDiariaQuarto(diariaId, novoQuartoId);
//
//    if (quartoAnteriorId != null
//        && !pernoiteRepository.hasActiveDiariaForQuarto(quartoAnteriorId)) {
//      quartoRepository.updateStatus(quartoAnteriorId, Quarto.Status.LIMPEZA);
//    }
//
//    quartoRepository.updateStatus(novoQuartoId, Quarto.Status.OCUPADO);
//
//    return pernoiteRepository.findDiariaById(diariaId);
//  }
//
//  // ── Diária – pessoas ────────────────────────────────────────────────────────
//
//  @Transactional
//  public Pernoite.Diaria addPessoasToDiaria(Long diariaId, List<Long> pessoaIds) {
//    if (pessoaIds == null || pessoaIds.isEmpty())
//      throw new IllegalArgumentException("Lista de pessoas não pode ser vazia.");
//    pernoiteRepository.getPernoiteIdByDiaria(diariaId);
//    pernoiteRepository.addPessoasToDiaria(diariaId, pessoaIds);
//    return pernoiteRepository.findDiariaById(diariaId);
//  }
//
//  @Transactional
//  public Pernoite.Diaria removePessoaFromDiaria(Long diariaId, Long pessoaId) {
//    pernoiteRepository.getPernoiteIdByDiaria(diariaId);
//    pernoiteRepository.removePessoaFromDiaria(diariaId, pessoaId);
//    return pernoiteRepository.findDiariaById(diariaId);
//  }
//
//  // ── Diária – pagamentos ─────────────────────────────────────────────────────
//
//  @Transactional
//  public Pernoite addPagamentosToPernoite(Long pernoiteId, List<Pagamento.Request> pagamentos) {
//    if (pagamentos == null || pagamentos.isEmpty())
//      throw new IllegalArgumentException("Lista de pagamentos não pode ser vazia.");
//    Long quartoId = pernoiteRepository.getQuartoIdByPernoite(pernoiteId);
//    for (Pagamento.Request pg : pagamentos) {
//      Pagamento criado = pagamentoRepository.create(pg);
//      pernoiteRepository.addPagamentoToPernoite(pernoiteId, criado.uuid());
//      relatorioRepository.registrarRelatorioDeConsumo(
//          criado,
//          relatorioRepository.getFuncionarioId(),
//          "Pernoite " + pernoiteId + " - Quarto " + quartoId);
//    }
//    return pernoiteRepository.findById(pernoiteId);
//  }
//
//  @Transactional
//  public Pernoite cancelPernoitePagamento(UUID pagamentoUuid, String motivo) {
//    if (motivo == null || motivo.isBlank())
//      throw new IllegalArgumentException("Motivo de cancelamento é obrigatório.");
//    Long pernoiteId = pernoiteRepository.findPernoiteIdByPagamentoUuid(pagamentoUuid);
//    pernoiteRepository.cancelPernoitePagamento(pagamentoUuid, motivo);
//    return pernoiteRepository.findById(pernoiteId);
//  }
//
//  // ── Diária – consumos ───────────────────────────────────────────────────────
//
//  @Transactional
//  public Pernoite.Diaria addConsumosToDiaria(Long diariaId, List<Item.Consumo.Request> consumos) {
//    System.out.println(consumos);
//    if (consumos == null || consumos.isEmpty())
//      throw new IllegalArgumentException("Lista de consumos não pode ser vazia.");
//    Long quartoId = pernoiteRepository.getQuartoIdByDiaria(diariaId);
//    for (Item.Consumo.Request req : consumos) {
//      UUID pagamentoId = null;
//      if (req.pagamento() != null) {
//        Pagamento criado = pagamentoRepository.create(req.pagamento());
//        pagamentoId = criado.uuid();
//        relatorioRepository.registrarRelatorioDeConsumo(
//            criado, relatorioRepository.getFuncionarioId(), "Consumo diária " + diariaId);
//      }
//      Long consumoQuartoId = req.quarto() != null ? req.quarto().id() : quartoId;
//      Long consumoId =
//          pernoiteRepository.insertConsumo(
//              pagamentoId,
//              req.item().id(),
//              req.quantidade(),
//              Boolean.TRUE.equals(req.despesa_pessoal()),
//              consumoQuartoId);
//      pernoiteRepository.addConsumoDiaria(diariaId, consumoId);
//    }
//    return pernoiteRepository.findDiariaById(diariaId);
//  }
//
//  @Transactional
//  public Pernoite.Diaria removeConsumoFromDiaria(Long diariaId, Long consumoId) {
//    pernoiteRepository.getPernoiteIdByDiaria(diariaId);
//    pernoiteRepository.removeConsumoFromDiaria(diariaId, consumoId);
//    return pernoiteRepository.findDiariaById(diariaId);
//  }
//
//  // ── Helpers de disponibilidade ──────────────────────────────────────────────
//
//  private void validarDisponibilidade(
//      Long quartoId,
//      LocalDateTime entrada,
//      LocalDateTime saida,
//      Long excludePernoiteId,
//      Long excludeReservaId) {
//    if (pernoiteRepository.isQuartoOcupado(quartoId)) {
//      throw new BusinessException("Quarto " + quartoId + " não está disponível.");
//    }
//    if (pernoiteRepository.hasConflito(
//        quartoId, entrada, saida, excludePernoiteId, excludeReservaId)) {
//      throw new ConflictException(
//          "Quarto " + quartoId + " possui ocupação conflitante no período informado.");
//    }
//  }
//
//  // ── Cálculo de preço por noite ──────────────────────────────────────────────
//
////  private float calcularValorTotal(
////      ReservaRepository.CategoriaCheckin catInfo,
////      LocalDate dataEntrada,
////      int noites,
////      int qtdPessoas) {
////    List<ReservaRepository.Sazonalidade> sazonalidades =
////        reservaRepository.findSazonalidades(catInfo.id());
////    List<Long> sazonIds =
// sazonalidades.stream().map(ReservaRepository.Sazonalidade::id).toList();
////    Map<Long, List<Categoria.ModeloOcupacao>> modelosOcupacao =
////        sazonIds.isEmpty() ? Map.of() : reservaRepository.findSazonModelosOcupacao(sazonIds);
////    Map<Long, List<Categoria.ModeloFixo>> modelosFixo =
////        sazonIds.isEmpty() ? Map.of() : reservaRepository.findSazonModelosFixo(sazonIds);
////    Categoria categoria =
////        categoriaRepository
////            .findCategoriasParaCalculo(List.of(catInfo.id()))
////            .getOrDefault(catInfo.id(), null);
////    if (categoria == null) return 0f;
////
////    float total = 0f;
////    for (int i = 0; i < noites; i++) {
////      total +=
////          (float)
////              calcularPrecoNoite(
////                  dataEntrada.plusDays(i),
////                  categoria,
////                  sazonalidades,
////                  modelosOcupacao,
////                  modelosFixo,
////                  qtdPessoas);
////    }
////    return total;
////  }
//
//
//
//
//
//  private LocalDateTime buildDateTime(LocalDate date, LocalTime time) {
//    return LocalDateTime.of(date, time != null ? time : LocalTime.MIDNIGHT);
//  }
// }
