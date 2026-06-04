package saas.hotel.istoepousada.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.*;
import saas.hotel.istoepousada.repository.HospedagemRepository;
import saas.hotel.istoepousada.repository.QuartoRepository;

@Service
public class HospedagemService {
  private static final Logger log = LoggerFactory.getLogger(HospedagemService.class);
  private final HospedagemRepository hospedagemRepository;
  private final PagamentoService pagamentoService;
  private final PessoaService pessoaService;
  private final CalcularPrecoService calcularPrecoService;
  private final QuartoRepository quartoRepository;

  public HospedagemService(
      HospedagemRepository hospedagemRepository,
      PagamentoService pagamentoService,
      PessoaService pessoaService,
      CalcularPrecoService calcularPrecoService,
      QuartoRepository quartoRepository) {
    this.hospedagemRepository = hospedagemRepository;
    this.pagamentoService = pagamentoService;
    this.pessoaService = pessoaService;
    this.calcularPrecoService = calcularPrecoService;
    this.quartoRepository = quartoRepository;
  }

  // ── Quarto / Disponibilidade ─────────────────────────────────────────────────

  public List<Quarto.Disponibilidade> verificarDisponibilidadeQuartos(
      LocalDate dataEntrada, LocalDate dataSaida) {

    LocalDateTime checkin  = dataEntrada.atStartOfDay();
    LocalDateTime checkout = dataSaida.atStartOfDay();

    List<Quarto> quartos = quartoRepository.buscarTodos();

    return quartos.stream()
        .map(quarto -> {
          Quarto.Status statusEfetivo;
          boolean disponivel;
          try {
            Quarto.Status statusFisico = quarto.status();

            if (statusFisico == Quarto.Status.OCUPADO
                || statusFisico == Quarto.Status.MANUTENCAO
                || statusFisico == Quarto.Status.LIMPEZA
                || statusFisico == Quarto.Status.FORA_DE_SERVICO) {
              statusEfetivo = statusFisico;
              disponivel    = false;
            } else {
              boolean temConflito =
                  hospedagemRepository.isQuartoDisponivel(quarto.id(), checkin, checkout, null);
              if (temConflito) {
                statusEfetivo = Quarto.Status.RESERVADO;
                disponivel    = false;
              } else {
                statusEfetivo = Quarto.Status.DISPONIVEL;
                disponivel    = true;
              }
            }
          } catch (Exception e) {
            log.warn("Erro ao verificar disponibilidade do quarto {}: {}", quarto.id(), e.getMessage());
            statusEfetivo = quarto.status();
            disponivel    = false;
          }
          return new Quarto.Disponibilidade(quarto.id(), quarto.descricao(), statusEfetivo, disponivel);
        })
        .toList();
  }

  public Boolean isQuartoDisponivel(
      Long quartoId, LocalDateTime checkin, LocalDateTime checkout, Long hospedagemIdExcluido) {
    log.info(
        "Validando disponibilidade do quarto {} para checkin {} e checkout {}",
        quartoId,
        checkin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
        checkout.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    var statusAtual = hospedagemRepository.statusQuarto(quartoId);
    if (statusAtual == Quarto.Status.OCUPADO) {
      log.info("Quarto não disponivel: STATUS {}", Quarto.Status.OCUPADO);
      throw new IllegalStateException("Quarto não disponivel: STATUS " + Quarto.Status.OCUPADO);
    }
    var temConflito = hospedagemRepository.isQuartoDisponivel(quartoId, checkin, checkout, hospedagemIdExcluido);
    log.info("Conflito encontrado: {}", temConflito);
    if (temConflito) {
      log.info("Quarto não disponivel para as datas informadas [{}->{}]", checkin, checkout);
      throw new IllegalArgumentException(
              "Quarto não disponivel nas datas informadas [" + checkin + "->" + checkout + "]");
    }
    return true;
  }

  // ── Diárias ──────────────────────────────────────────────────────────────────

  public List<Hospedagem.Diaria> listarDiarias(Long hospedagemId) {
    return hospedagemRepository.listarDiarias(hospedagemId);
  }

  public void adicionarDiarias(Long hospedagemId, List<Hospedagem.Diaria.Request> diarias) {
    Set<Long> todasPessoasIds =
        diarias.stream()
            .filter(d -> d.pessoas() != null)
            .flatMap(d -> d.pessoas().stream())
            .collect(Collectors.toSet());
    Map<Long, LocalDate> dataNascimentoPorPessoa =
        todasPessoasIds.isEmpty() ? Map.of() : pessoaService.findDataNascimentoByIds(todasPessoasIds);

    Map<Long, LocalDateTime> minCheckinPorQuarto = new HashMap<>();
    Map<Long, LocalDateTime> maxCheckoutPorQuarto = new HashMap<>();
    for (var diaria : diarias) {
      minCheckinPorQuarto.merge(
          diaria.quarto_id(), diaria.checkin(), (a, b) -> a.isBefore(b) ? a : b);
      maxCheckoutPorQuarto.merge(
          diaria.quarto_id(), diaria.checkout(), (a, b) -> a.isAfter(b) ? a : b);
    }
    minCheckinPorQuarto.forEach(
        (quartoId, minCheckin) ->
            isQuartoDisponivel(quartoId, minCheckin, maxCheckoutPorQuarto.get(quartoId), hospedagemId));

    Set<String> existentes = hospedagemRepository.buscarChavesDiariasExistentes(hospedagemId);

    List<CalcularPreco.Request> calcularPrecoRequests = new ArrayList<>();
    List<Hospedagem.Diaria.Request> diariasNaoCadastradas = new ArrayList<>();

    diarias.forEach(
        diaria -> {
          String chave = diaria.quarto_id() + "_" + diaria.checkin() + "_" + diaria.checkout();
          if (existentes.contains(chave)) {
            log.info(
                "Diária [{}>{}] já existe para a hospedagem {} e quarto {}",
                diaria.checkin(),
                diaria.checkout(),
                hospedagemId,
                diaria.quarto_id());
            return;
          }

          List<LocalDate> datasNascimento =
              diaria.pessoas() == null
                  ? List.of()
                  : diaria.pessoas().stream()
                      .peek(
                          pessoaId ->
                              log.info(
                                  "Adicionando pessoa {} para a hospedagem {} e quarto {}",
                                  pessoaId,
                                  hospedagemId,
                                  diaria.quarto_id()))
                      .map(dataNascimentoPorPessoa::get)
                      .filter(Objects::nonNull)
                      .toList();

          calcularPrecoRequests.add(
              new CalcularPreco.Request(
                  diaria.quarto_id(),
                  diaria.checkin().toLocalDate(),
                  diaria.checkout().toLocalDate(),
                  datasNascimento,
                  null,
                  null));
          diariasNaoCadastradas.add(diaria);
        });

    if (diariasNaoCadastradas.isEmpty()) return;

    var resultadoCalculo = calcularPrecoService.calcularPreco(calcularPrecoRequests);
    List<Double> valores =
        resultadoCalculo.stream().map(CalcularPreco.Resultado::valor_total).toList();
    hospedagemRepository.adicionarDiarias(hospedagemId, diariasNaoCadastradas, valores);
  }

  // ── Status ───────────────────────────────────────────────────────────────────

  public void validarTransicaoDeStatus(Hospedagem.Status anterior, Hospedagem.Status novo) {
    if (anterior == novo) return;
    Map<Hospedagem.Status, Set<Hospedagem.Status>> transicoesPermitidas =
        Map.ofEntries(
            Map.entry(
                Hospedagem.Status.ORCAMENTO,
                EnumSet.of(
                    Hospedagem.Status.ORCAMENTO_CANCELADO, Hospedagem.Status.RESERVA_SOLICITADA)),
            Map.entry(
                Hospedagem.Status.ORCAMENTO_CANCELADO, EnumSet.of(Hospedagem.Status.ORCAMENTO)),
            Map.entry(
                Hospedagem.Status.RESERVA_SOLICITADA,
                EnumSet.of(Hospedagem.Status.RESERVA_ATIVA, Hospedagem.Status.RESERVA_CANCELADA)),
            Map.entry(
                Hospedagem.Status.RESERVA_ATIVA,
                EnumSet.of(
                    Hospedagem.Status.RESERVA_CANCELADA,
                    Hospedagem.Status.RESERVA_AUSENTE,
                    Hospedagem.Status.PERNOITE_ATIVO)),
            Map.entry(
                Hospedagem.Status.RESERVA_AUSENTE, EnumSet.of(Hospedagem.Status.RESERVA_ATIVA)),
            Map.entry(
                Hospedagem.Status.PERNOITE_ATIVO,
                EnumSet.of(
                    Hospedagem.Status.PERNOITE_CANCELADO,
                    Hospedagem.Status.PERNOITE_FINALIZADO,
                    Hospedagem.Status.PERNOITE_FINALIZADO_PAGAMENTO_PENDENTE)),
            Map.entry(
                Hospedagem.Status.PERNOITE_FINALIZADO_PAGAMENTO_PENDENTE,
                EnumSet.of(Hospedagem.Status.PERNOITE_FINALIZADO)),
            Map.entry(
                Hospedagem.Status.DAY_USE_SOLICITADO,
                EnumSet.of(
                    Hospedagem.Status.DAY_USE_AUSENTE,
                    Hospedagem.Status.DAY_USE_ATIVO,
                    Hospedagem.Status.DAY_USE_CANCELADO)),
            Map.entry(
                Hospedagem.Status.DAY_USE_ATIVO,
                EnumSet.of(
                    Hospedagem.Status.DAY_USE_FINALIZADO,
                    Hospedagem.Status.DAY_USE_FINALIZADO_PAGAMENTO_PENDENTE)),
            Map.entry(
                Hospedagem.Status.DAY_USE_FINALIZADO_PAGAMENTO_PENDENTE,
                EnumSet.of(Hospedagem.Status.DAY_USE_FINALIZADO)));

    Set<Hospedagem.Status> estadosFinais =
        EnumSet.of(
            Hospedagem.Status.RESERVA_CANCELADA,
            Hospedagem.Status.PERNOITE_CANCELADO,
            Hospedagem.Status.PERNOITE_FINALIZADO,
            Hospedagem.Status.DAY_USE_CANCELADO,
            Hospedagem.Status.DAY_USE_FINALIZADO);

    if (estadosFinais.contains(anterior)) {
      throw new IllegalStateException(
          "Status " + anterior + " é um estado final e não pode ser alterado.");
    }
    Set<Hospedagem.Status> permitidos = transicoesPermitidas.get(anterior);
    if (permitidos == null || !permitidos.contains(novo)) {
      throw new IllegalStateException("Transição de status inválida: " + anterior + " -> " + novo);
    }
  }

  public void alterarStatus(Long hospedagemId, Hospedagem.Status status) {
    hospedagemRepository.alterarStatus(hospedagemId, status, getFuncionarioId());
    log.info("Status da hospedagem: [{}] alterado para: [{}]", hospedagemId, status);
  }

  /** Altera o status validando a transição a partir do status atual da hospedagem. */
  @Transactional
  public void alterarStatusComValidacao(Long hospedagemId, Hospedagem.Status novoStatus) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), novoStatus);
    alterarStatus(hospedagemId, novoStatus);
  }

  // ── Pessoas ──────────────────────────────────────────────────────────────────
  public List<Pessoa.DadosPrincipais> buscarPessoasHospedagem(Long hospedagemId){
    return  pessoaService.buscarByHospedagemId(hospedagemId);
  }

  public void removerPessoas(Long hospedagemId, List<Long> pessoasIds) {
    hospedagemRepository.buscarPorId(hospedagemId);
    List<Long> pessoasExistentes =
        hospedagemRepository.filtrarPessoasExistentes(hospedagemId, pessoasIds);
    if (pessoasExistentes.isEmpty()) return;
    hospedagemRepository.removerPessoas(hospedagemId, pessoasExistentes);
    log.info("Pessoas {} removidas da hospedagem {}", pessoasExistentes, hospedagemId);
  }

  public void adicionarPessoas(Long hospedagemId, List<Long> pessoasIds) {
    List<Long> pessoasPendentes =
        hospedagemRepository.filtrarPessoasDuplicadas(hospedagemId, pessoasIds);
    if (pessoasPendentes.isEmpty()) return;
    hospedagemRepository.adicionarPessoas(hospedagemId, pessoasPendentes);
  }

  // ── Pagamentos ───────────────────────────────────────────────────────────────

  public void adicionarHospedagemPagamento(Long hospedagemId, List<UUID> pagamentosUUID) {
    hospedagemRepository.adicionarHospedagemPagamento(hospedagemId, pagamentosUUID);
  }

  public void adicionarHospedagemPagamento(Long hospedagemId, List<UUID> pagamentosUUID, Long grupoId) {
    hospedagemRepository.adicionarHospedagemPagamento(hospedagemId, pagamentosUUID, grupoId);
  }

  private void validarCamposPagamentoUnico(Pagamento.Request pagamento) {
    if (pagamento.tipo_pagamento() == null)
      throw new IllegalArgumentException("Forma de pagamento não informada");
    if (pagamento.nome_pagador() == null)
      throw new IllegalArgumentException("Nome do pagador não informado");
    if (pagamento.valor() == null)
      throw new IllegalArgumentException("Valor do pagamento não informado");
  }

  @Transactional
  public void adicionarPagamentoMultiplasHospedagens(List<Long> hospedagemIds, Pagamento.Request pagamento) {
    validarCamposPagamentoUnico(pagamento);
    var newPagamento = pagamentoService.criar(pagamento);
    List<UUID> pagamentosUUID = List.of(newPagamento.uuid());
    hospedagemIds.forEach(id -> adicionarHospedagemPagamento(id, pagamentosUUID));
    log.info("Pagamento {} adicionado às hospedagens {}", newPagamento.uuid(), hospedagemIds);
  }

  /** Cria um único pagamento e vincula a todas as hospedagens de um grupo, guardando o grupo. */
  @Transactional
  public void adicionarPagamentoGrupo(Long grupoId, Pagamento.Request pagamento) {
    validarCamposPagamentoUnico(pagamento);
    List<Long> hospedagemIds = hospedagemRepository.buscarHospedagemIdsPorGrupo(grupoId);
    if (hospedagemIds.isEmpty())
      throw new IllegalArgumentException("Grupo não encontrado ou sem reservas: " + grupoId);
    var newPagamento = pagamentoService.criar(pagamento);
    List<UUID> pagamentosUUID = List.of(newPagamento.uuid());
    hospedagemIds.forEach(id -> adicionarHospedagemPagamento(id, pagamentosUUID, grupoId));
    log.info("Pagamento {} adicionado ao grupo {} (hospedagens {})", newPagamento.uuid(), grupoId, hospedagemIds);
  }

  public void adicionarPagamentos(Long hospedagemId, Hospedagem.Request request) {
    if (request.pagamentos() != null && !request.pagamentos().isEmpty()) {
      List<UUID> pagamentosUUID = new ArrayList<>();
      request
          .pagamentos()
          .forEach(
              pagamento -> {
                var newPagamento = pagamentoService.criar(pagamento);
                pagamentosUUID.add(newPagamento.uuid());
              });
      adicionarHospedagemPagamento(hospedagemId, pagamentosUUID);
    }
  }

  // ── Cancelamento ─────────────────────────────────────────────────────────────

  public MotivoCancelamentoHospedagem buscarMotivoCancelamento(Long hospedagemId) {
    return hospedagemRepository.buscarMotivoCancelamento(hospedagemId);
  }

  public void adicionarMotivoCancelamento(MotivoCancelamentoHospedagem.Request request) {
    validarCamposMotivoCancelamento(request, false);
    hospedagemRepository.adicionarMotivoCancelamento(request, getFuncionarioId());
    log.info("Motivo cancelamento: [{}]", request.motivo_cancelamento());
  }

  public void editarMotivoCancelamento(MotivoCancelamentoHospedagem.Request request) {
    validarCamposMotivoCancelamento(request, true);
    hospedagemRepository.editarMotivoCancelamento(request);
    log.info("Motivo cancelamento: [{}]", request.motivo_cancelamento());
  }

  private void validarCamposMotivoCancelamento(
      MotivoCancelamentoHospedagem.Request request, Boolean isUpdate) {
    if (isUpdate) {
      if (request.id() == null || request.id() <= 0)
        throw new IllegalArgumentException("Id do motivo de cancelamento nao informado");
    }
    if (request.motivo_cancelamento() == null || request.motivo_cancelamento().isEmpty())
      throw new IllegalArgumentException("Motivo do cancelamento nao informado");
  }

  // ── Consumo ──────────────────────────────────────────────────────────────────

  public void adicionarConsumo(Long hospedagemId, Item.Consumo.Request request) {
    hospedagemRepository.buscarPorId(hospedagemId);
    validarCamposConsumo(request, false);
    UUID pagamentoId = null;
    if (request.pagamento() != null) {
      pagamentoId = pagamentoService.criar(request.pagamento()).uuid();
    }
    hospedagemRepository.adicionarConsumo(hospedagemId, request, pagamentoId, getFuncionarioId());
  }

  public void editarConsumo(Item.Consumo.Request request) {
    validarCamposConsumo(request, true);
    hospedagemRepository.editarConsumo(request);
  }

  public List<Item.Consumo> buscarConsumosPorHospedagem(Long hospedagemId) {
    return hospedagemRepository.buscarConsumosPorHospedagem(hospedagemId);
  }

  private void validarCamposConsumo(Item.Consumo.Request request, Boolean isUpdate) {
    if (isUpdate) {
      if (request.id() == null || request.id() <= 0)
        throw new IllegalArgumentException("Id do consumo nao informado");
    }
    if (request.despesa_pessoal() == null)
      throw new IllegalArgumentException("Despesa pessoal nao informada");
    if (request.item() == null || request.item().id() == null || request.item().id() <= 0)
      throw new IllegalArgumentException("Item nao informado");
    if (request.quantidade() == null || request.quantidade() <= 0)
      throw new IllegalArgumentException("Quantidade nao informada");
    if (request.quarto() != null) {
      if (request.quarto().id() == null || request.quarto().id() <= 0)
        throw new IllegalArgumentException("Quarto nao informado");
    }
  }

  // ── Orçamento ────────────────────────────────────────────────────────────────

  private void validarCamposOrcamento(Orcamento.Request request, Boolean isUpdate) {
    if (request == null)
      throw new IllegalArgumentException("Requisição de Orcamento não informada");

    if (isUpdate) {
      if (request.id() == null || request.id() <= 0) {
        throw new IllegalArgumentException("Id de orçamento não informado ou inválido");
      }
    }
    if (request.nome_solicitante() == null || request.nome_solicitante().isEmpty()) {
      throw new IllegalArgumentException("Nome do solicitante não informado");
    }
  }

  private void validarCamposPessoasOrcamento(
      List<Hospedagem.PessoaHospedagemOrcamento.Request> pessoas, Boolean isUpdate) {
    pessoas.forEach(
        request -> {
          if (request == null)
            throw new IllegalArgumentException("Requisição de pessoas no orçamento não informada");
          if (isUpdate) {
            if (request.id() == null || request.id() <= 0)
              throw new IllegalArgumentException("Id do orçamento não informado ou inválido");
          }
          if (request.nome() == null || request.nome().isEmpty())
            throw new IllegalArgumentException("Nome da pessoa não informado");
          if (request.data_nascimento() == null)
            throw new IllegalArgumentException("Data de nascimento não informada");
          else if (request.data_nascimento().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Data de nascimento inválida");
        });
  }

  public void editarOrcamento(Orcamento.Request orcamentoRequest) {
    validarCamposOrcamento(orcamentoRequest, true);
    hospedagemRepository.editarOrcamento(orcamentoRequest);
  }

  public void adicionarPessoasHospedagemOrcamentoSolicitacao(
      Long hospedagemId, List<Hospedagem.PessoaHospedagemOrcamento.Request> pessoas) {
    validarCamposPessoasOrcamento(pessoas, false);
    hospedagemRepository.adicionarPessoasHospedagemOrcamento(hospedagemId, pessoas);
  }

  public void removerPessoasOrcamento(List<Long> pessoasOrcamentoIds) {
    hospedagemRepository.removerPessoasOrcamento(pessoasOrcamentoIds);
  }

  public List<Orcamento> buscarOrcamento(Long orcamentoId, String nomeSolicitante) {
    return hospedagemRepository.buscarOrcamento(orcamentoId, nomeSolicitante);
  }

  private void validarCamposHospedagem(Hospedagem.Request request, Boolean isUpdate) {
    if (isUpdate) {
      if (request.hospedagem_id() == null || request.hospedagem_id() <= 0) {
        throw new IllegalArgumentException("Id da hospedagem não informado ou inválido");
      }
    }
    if (!request.status().equals(Hospedagem.Status.RESERVA_SOLICITADA)) {
      if (request.quarto_id() == null)
        throw new IllegalArgumentException("E necessário informar o quarto da hospedagem.");
    }
    if (request.data_hora_checkin() == null)
      throw new IllegalArgumentException("E necessário informar a data de checkin da hospedagem.");
    if (request.data_hora_checkout() == null)
      throw new IllegalArgumentException("E necessário informar a data de checkout da hospedagem.");
    if (request.data_hora_checkin().isAfter(request.data_hora_checkout()))
      throw new IllegalArgumentException("A data de checkin deve ser anterior a data de checkout.");
  }

  // ── Flow: Orçamento ──────────────────────────────────────────────────────────
  @Transactional
  public void criarOrcamento(Orcamento.Request orcamento) {
    validarCamposOrcamento(orcamento, false);
    var orcamentoId = hospedagemRepository.adicionarOrcamento(orcamento, getFuncionarioId());

    List<Long> hospedagensIds = new ArrayList<>();

    orcamento
        .hospedagens()
        .forEach(
            hospedagem -> {
              validarCamposHospedagem(hospedagem, false);
              isQuartoDisponivel(
                  hospedagem.quarto_id(),
                  hospedagem.data_hora_checkin(),
                  hospedagem.data_hora_checkout(),
                  null);
              var insertRequest =
                  new Hospedagem.Request(
                      null,
                      hospedagem.quarto_id(),
                      Hospedagem.Status.ORCAMENTO,
                      hospedagem.data_hora_checkin(),
                      hospedagem.data_hora_checkout(),
                      null,
                      null,
                      hospedagem.observacao(),
                      hospedagem.valor_total(),
                      hospedagem.pessoas_orcamento(),
                      null,
                      null);
              Hospedagem newHospedagem =
                  hospedagemRepository.insertHospedagem(insertRequest, getFuncionarioId());
              validarCamposPessoasOrcamento(hospedagem.pessoas_orcamento(), false);
              adicionarPessoasHospedagemOrcamentoSolicitacao(
                  newHospedagem.id(), hospedagem.pessoas_orcamento());
              hospedagensIds.add(newHospedagem.id());
            });
    hospedagemRepository.vincularHospedagensOrcamento(hospedagensIds, orcamentoId);
  }

  @Transactional
  public void cancelarOrcamento(Long orcamentoId, MotivoCancelamentoHospedagem.Request motivo) {
    Orcamento orcamento = hospedagemRepository.buscarOrcamento(orcamentoId, null).getFirst();
    orcamento
        .hospedagens()
        .forEach(
            hospedagem -> {
              hospedagemRepository.buscarPorId(hospedagem.id());
              validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.ORCAMENTO_CANCELADO);
              adicionarMotivoCancelamento(
                  new MotivoCancelamentoHospedagem.Request(
                      hospedagem.id(), null, motivo.motivo_cancelamento()));
              alterarStatus(hospedagem.id(), Hospedagem.Status.ORCAMENTO_CANCELADO);
            });
    log.info(
        "Orcamento ID:{} {} cancelado. Hospedagens: {}",
        orcamentoId,
        orcamento.nome_solicitante(),
        orcamento.hospedagens());
  }

  // ── Flow: Reserva ────────────────────────────────────────────────────────────

  @Transactional
  public void solicitarReserva(Hospedagem.Request request) {
    validarCamposHospedagem(request, false);
    Hospedagem hospedagem = hospedagemRepository.insertHospedagem(request, null);
    alterarStatus(hospedagem.id(), Hospedagem.Status.RESERVA_SOLICITADA);
    adicionarPessoasHospedagemOrcamentoSolicitacao(hospedagem.id(), request.pessoas_orcamento());
  }

  @Transactional
  protected void calcularDiarias(Long hospedagemId, Hospedagem.Request request) {
    List<Hospedagem.Diaria.Request> diarias = new ArrayList<>();
    var dataInicio = request.data_hora_checkin();
    int total_diarias =
        Period.between(dataInicio.toLocalDate(), request.data_hora_checkout().toLocalDate())
            .getDays();
    for (int i = 0; i < total_diarias; i++) {
      diarias.add(
          new Hospedagem.Diaria.Request(
              request.quarto_id(), dataInicio, dataInicio.plusDays(1), request.pessoas()));
      dataInicio = dataInicio.plusDays(1);
    }
    adicionarDiarias(hospedagemId, diarias);
  }

  @Transactional
  public void ativarReserva(List<Hospedagem.Request> requests, Boolean pagamentoUnico) {
    // Phase 1 — activate every reservation (no payment when pagamentoUnico=true)
    List<Long> resolvedIds = new ArrayList<>();

    for (Hospedagem.Request request : requests) {
      validarCamposHospedagem(request, false);

      final Long resolvedId;
      final Hospedagem.Status statusAtual;
      Long hospedagemId = request.hospedagem_id();

      if (hospedagemId != null) {
        Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
        if (hospedagem.status().equals(Hospedagem.Status.RESERVA_ATIVA))
          throw new IllegalArgumentException("Reserva já ativa para esse quarto e data.");
        resolvedId = hospedagemId;
        statusAtual = hospedagem.status();
      } else {
        resolvedId = hospedagemRepository.insertHospedagemId(request, getFuncionarioId());
        statusAtual = request.status();
      }
      calcularDiarias(resolvedId, request);
      validarTransicaoDeStatus(statusAtual, Hospedagem.Status.RESERVA_ATIVA);

      if (request.pessoas() != null && !request.pessoas().isEmpty())
        adicionarPessoas(resolvedId, request.pessoas());

      if (!Boolean.TRUE.equals(pagamentoUnico)) {
        if (request.pagamentos() != null && !request.pagamentos().isEmpty()) {
          validarCamposPagamento(request);
          adicionarPagamentos(resolvedId, request);
        }
      }

      alterarStatus(resolvedId, Hospedagem.Status.RESERVA_ATIVA);

      if (request.data_hora_checkin().toLocalDate().equals(LocalDate.now())) {
        var statusQuarto = hospedagemRepository.statusQuarto(request.quarto_id());
        if (!statusQuarto.equals(Quarto.Status.OCUPADO)) {
          quartoRepository.updateStatus(request.quarto_id(), Quarto.Status.RESERVADO);
        }
      }

      resolvedIds.add(resolvedId);
    }

    // Phase 2 — link all reservations in a group when more than one was activated
    Long grupoId = null;
    if (resolvedIds.size() > 1) {
      grupoId = hospedagemRepository.criarGrupoReserva(getFuncionarioId());
      hospedagemRepository.vincularHospedagensGrupo(resolvedIds, grupoId);
      log.info("Grupo {} criado para as hospedagens {}", grupoId, resolvedIds);
    }

    // Phase 3 — create one payment and link it to every reservation (carrying the group when present)
    if (Boolean.TRUE.equals(pagamentoUnico)) {
      Hospedagem.Request firstWithPagamentos = requests.stream()
          .filter(r -> r.pagamentos() != null && !r.pagamentos().isEmpty())
          .findFirst()
          .orElse(null);

      if (firstWithPagamentos != null) {
        validarCamposPagamento(firstWithPagamentos);
        List<UUID> pagamentosUUID = new ArrayList<>();
        firstWithPagamentos.pagamentos().forEach(pagamento -> {
          var newPagamento = pagamentoService.criar(pagamento);
          pagamentosUUID.add(newPagamento.uuid());
        });
        final Long grupoIdFinal = grupoId;
        resolvedIds.forEach(id -> adicionarHospedagemPagamento(id, pagamentosUUID, grupoIdFinal));
      }
    }
  }

  @Transactional
  public void cancelarReserva(Long hospedagemId, MotivoCancelamentoHospedagem.Request motivo) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.RESERVA_CANCELADA);
    adicionarMotivoCancelamento(motivo);
    alterarStatus(hospedagemId, Hospedagem.Status.RESERVA_CANCELADA);
  }

  @Transactional
  public void marcarReservaAusente(Long hospedagemId) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.RESERVA_AUSENTE);
    alterarStatus(hospedagemId, Hospedagem.Status.RESERVA_AUSENTE);
  }

  // ── Flow: Hospedagem (Pernoite) ──────────────────────────────────────────────

  @Transactional
  public void ativarPernoite(Long hospedagemId, Hospedagem.Request request) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.PERNOITE_ATIVO);
    if (request.pessoas() == null || request.pessoas().isEmpty())
      throw new IllegalArgumentException("A hospedagem deve ter pelo menos uma pessoa.");
    validarCamposPagamento(request);

    // TODO: fazer lista de diarias e para cada diaria adicionar.
    List<Hospedagem.Diaria.Request> diarias =
        List.of(
            new Hospedagem.Diaria.Request(
                request.quarto_id(),
                request.data_hora_checkin(),
                request.data_hora_checkout(),
                request.pessoas()));
    adicionarDiarias(hospedagemId, diarias);
    adicionarPessoas(hospedagemId, request.pessoas());
    adicionarPagamentos(hospedagemId, request);
    alterarStatus(hospedagemId, Hospedagem.Status.PERNOITE_ATIVO);
  }

  @Transactional
  public void cancelarPernoite(Long hospedagemId, MotivoCancelamentoHospedagem.Request motivo) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.PERNOITE_CANCELADO);
    adicionarMotivoCancelamento(motivo);
    alterarStatus(hospedagemId, Hospedagem.Status.PERNOITE_CANCELADO);
  }

  @Transactional
  public void finalizarPernoite(Long hospedagemId) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.PERNOITE_FINALIZADO);
    alterarStatus(hospedagemId, Hospedagem.Status.PERNOITE_FINALIZADO);
  }

  @Transactional
  public void finalizarPernoitePagamentoPendente(Long hospedagemId) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(
        hospedagem.status(), Hospedagem.Status.PERNOITE_FINALIZADO_PAGAMENTO_PENDENTE);
    alterarStatus(hospedagemId, Hospedagem.Status.PERNOITE_FINALIZADO_PAGAMENTO_PENDENTE);
  }

  // ── Flow: Day Use ────────────────────────────────────────────────────────────

  @Transactional
  public Hospedagem solicitarDayUse(Hospedagem.Request request) {
    if (request.quarto_id() == null)
      throw new IllegalArgumentException("E necessário informar o quarto da hospedagem.");
    if (request.data_hora_checkin() == null)
      throw new IllegalArgumentException("E necessário informar a data de checkin da hospedagem.");
    if (request.data_hora_checkout() == null)
      throw new IllegalArgumentException("E necessário informar a data de checkout da hospedagem.");
    if (request.data_hora_checkin().isAfter(request.data_hora_checkout()))
      throw new IllegalArgumentException("A data de checkin deve ser anterior a data de checkout.");
    isQuartoDisponivel(
        request.quarto_id(), request.data_hora_checkin(), request.data_hora_checkout(), null);

    var insertRequest =
        new Hospedagem.Request(
            null,
            request.quarto_id(),
            Hospedagem.Status.DAY_USE_SOLICITADO,
            request.data_hora_checkin(),
            request.data_hora_checkout(),
            null,
            null,
            request.observacao(),
            request.valor_total(),
            null,
            null,
            null);
    return hospedagemRepository.insertHospedagem(insertRequest, getFuncionarioId());
  }

  @Transactional
  public void ativarDayUse(Long hospedagemId, Hospedagem.Request request) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.DAY_USE_ATIVO);
    validarCamposPagamento(request);
    if (request.pessoas() != null && !request.pessoas().isEmpty())
      adicionarPessoas(hospedagemId, request.pessoas());
    adicionarPagamentos(hospedagemId, request);
    alterarStatus(hospedagemId, Hospedagem.Status.DAY_USE_ATIVO);
  }

  @Transactional
  public void cancelarDayUse(Long hospedagemId, MotivoCancelamentoHospedagem.Request motivo) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.DAY_USE_CANCELADO);
    adicionarMotivoCancelamento(motivo);
    alterarStatus(hospedagemId, Hospedagem.Status.DAY_USE_CANCELADO);
  }

  @Transactional
  public void marcarDayUseAusente(Long hospedagemId) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.DAY_USE_AUSENTE);
    alterarStatus(hospedagemId, Hospedagem.Status.DAY_USE_AUSENTE);
  }

  @Transactional
  public void finalizarDayUse(Long hospedagemId) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.DAY_USE_FINALIZADO);
    alterarStatus(hospedagemId, Hospedagem.Status.DAY_USE_FINALIZADO);
  }

  @Transactional
  public void finalizarDayUsePagamentoPendente(Long hospedagemId) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    validarTransicaoDeStatus(
        hospedagem.status(), Hospedagem.Status.DAY_USE_FINALIZADO_PAGAMENTO_PENDENTE);
    alterarStatus(hospedagemId, Hospedagem.Status.DAY_USE_FINALIZADO_PAGAMENTO_PENDENTE);
  }

  // ── Edição ───────────────────────────────────────────────────────────────────

  @Transactional
  public Hospedagem editarHospedagem(Hospedagem.Request request) {
    hospedagemRepository.buscarPorId(request.hospedagem_id());
    return withDetails(hospedagemRepository.editarHospedagem(request));
  }

  @Transactional
  public Hospedagem editarReserva(Long hospedagemId, Hospedagem.Request request) {
    if (hospedagemId == null)
      throw new IllegalArgumentException("Id da hospedagem é obrigatório.");

    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);

    if (!EnumSet.of(Hospedagem.Status.RESERVA_SOLICITADA, Hospedagem.Status.RESERVA_ATIVA)
        .contains(hospedagem.status())) {
      throw new IllegalStateException(
          "Status " + hospedagem.status() + " não permite edição da reserva.");
    }

    if (request.data_hora_checkin() != null && request.data_hora_checkout() != null
        && !request.data_hora_checkin().isBefore(request.data_hora_checkout())) {
      throw new IllegalArgumentException("A data de checkin deve ser anterior à data de checkout.");
    }

    boolean alterouPeriodo = request.quarto_id() != null
        || request.data_hora_checkin() != null
        || request.data_hora_checkout() != null;

    if (alterouPeriodo) {
      Long quartoIdFinal = request.quarto_id() != null
          ? request.quarto_id()
          : hospedagemRepository.buscarQuartoId(hospedagemId);
      LocalDateTime checkinFinal = request.data_hora_checkin() != null
          ? request.data_hora_checkin()
          : hospedagem.data_hora_checkin();
      LocalDateTime checkoutFinal = request.data_hora_checkout() != null
          ? request.data_hora_checkout()
          : hospedagem.data_hora_checkout();

      isQuartoDisponivel(quartoIdFinal, checkinFinal, checkoutFinal, hospedagemId);

      if (hospedagem.status().equals(Hospedagem.Status.RESERVA_ATIVA)) {
        List<Long> pessoasIds = hospedagemRepository.buscarPessoasIds(hospedagemId);
        hospedagemRepository.deletarDiarias(hospedagemId);
        if (!pessoasIds.isEmpty()) {
          Hospedagem.Request reqDiarias =
              new Hospedagem.Request(
                  hospedagemId,
                  quartoIdFinal,
                  hospedagem.status(),
                  checkinFinal,
                  checkoutFinal,
                  pessoasIds,
                  null,
                  null,
                  0.0,
                  null,
                  null,
                  null);
          calcularDiarias(hospedagemId, reqDiarias);
        }
      }
    }

    return withDetails(hospedagemRepository.editarReserva(hospedagemId, request, getFuncionarioId()));
  }

  // ── Consultas ────────────────────────────────────────────────────────────────

  public List<Hospedagem> buscar(
      List<Hospedagem.Status> statuses,
      LocalDate data,
      Integer mes,
      Integer ano,
      String nomeTitular) {
    List<Hospedagem> base = hospedagemRepository.buscar(statuses, data, mes, ano, nomeTitular);
    if (base.isEmpty()) return List.of();
    return withDetailsBatch(base);
  }

  /** Reservas de um quarto, paginadas. periodo: "anteriores" | "proximas" | null (mês/ano). */
  public PageResult<Hospedagem> buscarPorQuarto(
      Long quartoId, Integer mes, Integer ano, String periodo, int page, int size) {
    long total = hospedagemRepository.contarPorQuarto(quartoId, mes, ano, periodo);
    List<Hospedagem> base = hospedagemRepository.buscarPorQuarto(quartoId, mes, ano, periodo, page, size);
    List<Hospedagem> content = base.isEmpty() ? List.of() : withDetailsBatch(base);
    int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    return new PageResult<>(content, page, size, total, totalPages);
  }

  private List<Hospedagem> withDetailsBatch(List<Hospedagem> hospedagens) {
    List<Long> ids = hospedagens.stream().map(Hospedagem::id).toList();

    Map<Long, List<Hospedagem.Diaria>>                    diariasMap    = hospedagemRepository.listarDiariasBatch(ids);
    Map<Long, List<Item.Consumo>>                         consumosMap   = hospedagemRepository.buscarConsumosBatch(ids);
    Map<Long, List<Pagamento>>                            pagamentosMap = pagamentoService.buscarPorHospedagemIds(ids);
    Map<Long, Quarto>                                     quartosMap    = quartoRepository.buscarPorHospedagemIds(ids);
    Map<Long, List<Pessoa.DadosPrincipais>>               pessoasMap    = pessoaService.buscarByHospedagemIds(ids);
    Map<Long, List<Hospedagem.PessoaHospedagemOrcamento>> pessoasOrcMap = hospedagemRepository.buscarPessoasOrcamentoBatch(ids);
    Map<Long, MotivoCancelamentoHospedagem>               motivosMap    = hospedagemRepository.buscarMotivoCancelamentoBatch(ids);

    return hospedagens.stream().map(h -> new Hospedagem(
        h.id(),
        h.funcionario(),
        quartosMap.get(h.id()),
        h.data_hora_registro(),
        h.data_hora_checkin(),
        h.data_hora_checkout(),
        h.status(),
        h.valor_total(),
        h.quantidade_diarias(),
        h.numero_diaria_atual(),
        h.observacao(),
        diariasMap.getOrDefault(h.id(), List.of()),
        consumosMap.getOrDefault(h.id(), List.of()),
        pagamentosMap.getOrDefault(h.id(), List.of()),
        pessoasMap.getOrDefault(h.id(), List.of()),
        pessoasOrcMap.getOrDefault(h.id(), List.of()),
        motivosMap.get(h.id()),
        h.grupo_id()
    )).toList();
  }

  public Hospedagem buscarPorId(Long id) {
    return withDetails(hospedagemRepository.buscarPorId(id));
  }

  private Hospedagem withDetails(Hospedagem hospedagem) {
    var diarias = listarDiarias(hospedagem.id());
    var consumos = buscarConsumosPorHospedagem(hospedagem.id());
    var pagamentos = pagamentoService.buscarPorHospedagemId(hospedagem.id());
    var quarto = quartoRepository.buscarPorHospedagemId(hospedagem.id());
    var pessoas = buscarPessoasHospedagem(hospedagem.id());

    List<Hospedagem.PessoaHospedagemOrcamento> pessoasOrcamento =
        hospedagemRepository.buscarPessoasHospedagemOrcamento(hospedagem.id());

    MotivoCancelamentoHospedagem motivo;

    try {
      motivo = buscarMotivoCancelamento(hospedagem.id());
    } catch (EmptyResultDataAccessException e) {
      motivo = null;
    }

    return new Hospedagem(
        hospedagem.id(),
        hospedagem.funcionario(),
        quarto,
        hospedagem.data_hora_registro(),
        hospedagem.data_hora_checkin(),
        hospedagem.data_hora_checkout(),
        hospedagem.status(),
        hospedagem.valor_total(),
        hospedagem.quantidade_diarias(),
        hospedagem.numero_diaria_atual(),
        hospedagem.observacao(),
        diarias,
        consumos,
        pagamentos,
        pessoas,
        pessoasOrcamento,
        motivo,
        hospedagem.grupo_id());
  }

  public Map<Long, Hospedagem> buscarAtivasPorQuartoNaData(LocalDate data) {
    return hospedagemRepository.buscarAtivasPorQuartoNaData(data);
  }

  public boolean temReservaAtivaParaQuartoHoje(Long quartoId) {
    return hospedagemRepository.temReservaAtivaParaQuartoHoje(quartoId);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  private Long getFuncionarioId() {
    return pessoaService.getFuncionarioIdFromRequest();
  }

  public void validarCamposPagamento(Hospedagem.Request request) {
    if (request.pagamentos() != null) {
      request
          .pagamentos()
          .forEach(
              pagamento -> {
                if (pagamento.tipo_pagamento() == null)
                  throw new IllegalArgumentException("Forma de pagamento não informada");
                if (pagamento.descricao() == null)
                  throw new IllegalArgumentException("Descrição do pagamento não informada");
                if (pagamento.valor() == null)
                  throw new IllegalArgumentException("Valor do pagamento não informado");
                if (pagamento.nome_pagador() == null)
                  throw new IllegalArgumentException("Nome do pagador não informado");
                if (pagamento.arquivo() != null) {
                  if (pagamento.arquivo().isEmpty())
                    throw new IllegalArgumentException("Arquivo do pagamento não informado");
                }
              });
    }
  }
}
