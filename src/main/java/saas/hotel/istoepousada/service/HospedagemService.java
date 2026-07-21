package saas.hotel.istoepousada.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
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
  private final RelatorioService relatorioService;

  public HospedagemService(
      HospedagemRepository hospedagemRepository,
      PagamentoService pagamentoService,
      PessoaService pessoaService,
      CalcularPrecoService calcularPrecoService,
      QuartoRepository quartoRepository,
      RelatorioService relatorioService) {
    this.hospedagemRepository = hospedagemRepository;
    this.pagamentoService = pagamentoService;
    this.pessoaService = pessoaService;
    this.calcularPrecoService = calcularPrecoService;
    this.quartoRepository = quartoRepository;
    this.relatorioService = relatorioService;
  }

  // ── Quarto / Disponibilidade ─────────────────────────────────────────────────

  public List<Quarto.Disponibilidade> verificarDisponibilidadeQuartos(
      LocalDate dataEntrada, LocalDate dataSaida) {

    LocalDateTime checkin = dataEntrada.atStartOfDay();
    LocalDateTime checkout = dataSaida.atStartOfDay();

    // Uma única query resolve o conflito por datas de todos os quartos (evita N+1).
    return hospedagemRepository.verificarDisponibilidadeLote(checkin, checkout).stream()
        .map(
            row -> {
              Quarto.Status statusFisico = row.statusFisico();
              Quarto.Status statusEfetivo;
              boolean disponivel;

              // Apenas estados físicos independentes de data bloqueiam o intervalo inteiro.
              // OCUPADO e LIMPEZA são estados transitórios do "agora": OCUPADO já está
              // representado pelas reservas/diárias e LIMPEZA se resolve em horas (o quarto
              // estará limpo muito antes de uma estadia futura). Ambos são resolvidos pela
              // verificação de conflito por datas — senão um quarto ocupado/em limpeza hoje
              // apareceria indisponível para datas futuras livres, divergindo do calendário.
              // Só MANUTENCAO e FORA_DE_SERVICO retiram o quarto de serviço por completo.
              if (statusFisico == Quarto.Status.MANUTENCAO
                  || statusFisico == Quarto.Status.FORA_DE_SERVICO) {
                statusEfetivo = statusFisico;
                disponivel = false;
              } else if (row.conflito()) {
                statusEfetivo = Quarto.Status.RESERVADO;
                disponivel = false;
              } else {
                statusEfetivo = Quarto.Status.DISPONIVEL;
                disponivel = true;
              }

              return new Quarto.Disponibilidade(
                  row.quartoId(), row.descricao(), statusEfetivo, disponivel);
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
    var temConflito =
        hospedagemRepository.isQuartoDisponivel(quartoId, checkin, checkout, hospedagemIdExcluido);
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
        todasPessoasIds.isEmpty()
            ? Map.of()
            : pessoaService.findDataNascimentoByIds(todasPessoasIds);

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
            isQuartoDisponivel(
                quartoId, minCheckin, maxCheckoutPorQuarto.get(quartoId), hospedagemId));

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

          // Meia diária: cobra 1 diária cheia (checkin → checkin+1) e divide por 2 no resultado.
          boolean meia = Boolean.TRUE.equals(diaria.meia_diaria());
          LocalDate dataEntrada = diaria.checkin().toLocalDate();
          LocalDate dataSaida = meia ? dataEntrada.plusDays(1) : diaria.checkout().toLocalDate();
          calcularPrecoRequests.add(
              new CalcularPreco.Request(
                  diaria.quarto_id(), dataEntrada, dataSaida, datasNascimento, null, null));
          diariasNaoCadastradas.add(diaria);
        });

    if (diariasNaoCadastradas.isEmpty()) return;

    var resultadoCalculo = calcularPrecoService.calcularPreco(calcularPrecoRequests);
    List<Double> valores = new ArrayList<>(resultadoCalculo.size());
    for (int i = 0; i < diariasNaoCadastradas.size(); i++) {
      Double pc = resultadoCalculo.get(i).valor_total();
      double v = pc == null ? 0.0 : pc;
      if (Boolean.TRUE.equals(diariasNaoCadastradas.get(i).meia_diaria())) v = v / 2.0;
      valores.add(v);
    }
    hospedagemRepository.adicionarDiarias(hospedagemId, diariasNaoCadastradas, valores);
  }

  /**
   * Substitui por completo as diárias da hospedagem ("Gerenciar Diárias"). Cada diária pode ter o
   * seu próprio quarto e o seu próprio conjunto de pessoas (usado no cálculo de preço por idade). O
   * preço de cada diária é recalculado e o {@code valor_total} da hospedagem passa a ser a soma das
   * diárias. Remove as diárias antigas via {@link HospedagemRepository#deletarDiarias}.
   */
  @Transactional
  public Hospedagem atualizarDiarias(Long hospedagemId, List<Hospedagem.Diaria.Request> diarias) {
    hospedagemRepository.buscarPorId(hospedagemId);
    if (diarias == null || diarias.isEmpty())
      throw new IllegalArgumentException("Informe ao menos uma diária.");
    diarias.forEach(
        d -> {
          if (d.quarto_id() == null)
            throw new IllegalArgumentException("Diária sem quarto informado.");
          if (d.checkin() == null || d.checkout() == null)
            throw new IllegalArgumentException("Diária sem data de checkin/checkout.");
          if (!d.checkin().isBefore(d.checkout()) && !d.meia_diaria())
            throw new IllegalArgumentException(
                "O checkin da diária deve ser anterior ao checkout.");
        });

    // Valida conflitos de data por quarto, ignorando a própria hospedagem (mover diárias entre
    // quartos da própria hospedagem é permitido). Não bloqueia pelo status físico OCUPADO porque o
    // quarto atual já está ocupado por esta hospedagem.
    Map<Long, LocalDateTime> minCheckinPorQuarto = new HashMap<>();
    Map<Long, LocalDateTime> maxCheckoutPorQuarto = new HashMap<>();
    for (var d : diarias) {
      minCheckinPorQuarto.merge(d.quarto_id(), d.checkin(), (a, b) -> a.isBefore(b) ? a : b);
      maxCheckoutPorQuarto.merge(d.quarto_id(), d.checkout(), (a, b) -> a.isAfter(b) ? a : b);
    }
    minCheckinPorQuarto.forEach(
        (quartoId, minCheckin) -> {
          boolean conflito =
              hospedagemRepository.isQuartoDisponivel(
                  quartoId, minCheckin, maxCheckoutPorQuarto.get(quartoId), hospedagemId);
          if (conflito)
            throw new IllegalArgumentException(
                "Quarto " + quartoId + " indisponível nas datas informadas.");
        });

    // Calcula o preço de cada diária (considerando as pessoas/idades de cada uma).
    List<Double> valores = precificarDiarias(diarias);

    // Substitui as diárias e recalcula o total da hospedagem.
    hospedagemRepository.deletarDiarias(hospedagemId);
    hospedagemRepository.adicionarDiarias(hospedagemId, diarias, valores);
    double totalDiarias = valores.stream().mapToDouble(Double::doubleValue).sum();

    // Reaplica o ajuste manual de preço (desconto/adicional) vigente sobre o novo total de diárias,
    // para que o desconto seja considerado ao adicionar/remover diárias.
    HospedagemNovoPreco novoPreco = hospedagemRepository.buscarNovoPreco(hospedagemId);
    double totalFinal = aplicarNovoPrecoAoTotal(totalDiarias, diarias.size(), novoPreco);
    if (novoPreco != null && novoPreco.valor_diaria() != null) {
      // Modo "valor por diária": todas as diárias passam a valer o valor definido.
      hospedagemRepository.sobrescreverValorDiarias(hospedagemId, novoPreco.valor_diaria());
    }
    hospedagemRepository.atualizarValorTotal(hospedagemId, totalFinal);
    if (novoPreco != null) {
      // Mantém o snapshot do ajuste coerente com a nova quantidade de diárias e total.
      HospedagemNovoPreco.Request snapshot =
          new HospedagemNovoPreco.Request(
              diarias.size(),
              novoPreco.quantidade_pessoas(),
              novoPreco.valor_diaria(),
              novoPreco.porcentagem(),
              novoPreco.valor_desconto(),
              totalFinal,
              null);
      hospedagemRepository.salvarNovoPreco(hospedagemId, snapshot, getFuncionarioId());
    }

    // Atualiza o período da hospedagem para abranger as novas diárias (menor checkin / maior
    // checkout), para que o card do quarto reflita as datas corretas.
    LocalDateTime novoCheckin =
        diarias.stream()
            .map(Hospedagem.Diaria.Request::checkin)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(null);
    LocalDateTime novoCheckout =
        diarias.stream()
            .map(Hospedagem.Diaria.Request::checkout)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null);
    if (novoCheckin != null && novoCheckout != null) {
      hospedagemRepository.atualizarPeriodo(hospedagemId, novoCheckin, novoCheckout);
    }

    log.info(
        "Diárias da hospedagem {} atualizadas: {} diárias, total {} (base {}), período {} -> {}",
        hospedagemId,
        diarias.size(),
        totalFinal,
        totalDiarias,
        novoCheckin,
        novoCheckout);
    return withDetails(hospedagemRepository.buscarPorId(hospedagemId));
  }

  /**
   * Aplica o ajuste manual de preço ("Gerenciar Preços") sobre um total base. O sinal já está
   * embutido em {@code porcentagem}/{@code valor_desconto} (negativo = desconto). Modos mutuamente
   * exclusivos: valor por diária, percentual ou valor absoluto sobre o total.
   */
  private double aplicarNovoPrecoAoTotal(double base, int qtdDiarias, HospedagemNovoPreco np) {
    if (np == null) return base;
    if (np.valor_diaria() != null) return np.valor_diaria() * qtdDiarias;
    if (np.porcentagem() != null) return base + (base * np.porcentagem() / 100.0);
    if (np.valor_desconto() != null) return base + np.valor_desconto();
    return base;
  }

  /**
   * Calcula o valor de cada diária (considerando as pessoas/idades de cada uma), respeitando a meia
   * diária (preço de uma diária cheia dividido por 2). Não persiste nada.
   */
  private List<Double> precificarDiarias(List<Hospedagem.Diaria.Request> diarias) {
    Set<Long> todasPessoasIds =
        diarias.stream()
            .filter(d -> d.pessoas() != null)
            .flatMap(d -> d.pessoas().stream())
            .collect(Collectors.toSet());
    Map<Long, LocalDate> dataNascimentoPorPessoa =
        todasPessoasIds.isEmpty()
            ? Map.of()
            : pessoaService.findDataNascimentoByIds(todasPessoasIds);

    List<CalcularPreco.Request> calcularPrecoRequests = new ArrayList<>();
    for (var d : diarias) {
      List<LocalDate> datasNascimento =
          d.pessoas() == null
              ? List.of()
              : d.pessoas().stream()
                  .map(dataNascimentoPorPessoa::get)
                  .filter(Objects::nonNull)
                  .toList();
      // Meia diária: checkin/checkout caem no mesmo dia → o cálculo retorna 1 diária; usamos a
      // data de entrada + 1 para garantir o preço de uma diária cheia e depois dividimos por 2.
      boolean meia = Boolean.TRUE.equals(d.meia_diaria());
      LocalDate dataEntrada = d.checkin().toLocalDate();
      LocalDate dataSaida = meia ? dataEntrada.plusDays(1) : d.checkout().toLocalDate();
      calcularPrecoRequests.add(
          new CalcularPreco.Request(
              d.quarto_id(), dataEntrada, dataSaida, datasNascimento, null, null));
    }
    List<Double> precosCheios =
        calcularPrecoService.calcularPreco(calcularPrecoRequests).stream()
            .map(CalcularPreco.Resultado::valor_total)
            .toList();
    List<Double> valores = new ArrayList<>(precosCheios.size());
    for (int i = 0; i < diarias.size(); i++) {
      boolean meia = Boolean.TRUE.equals(diarias.get(i).meia_diaria());
      double cheio = precosCheios.get(i) == null ? 0.0 : precosCheios.get(i);
      valores.add(meia ? cheio / 2.0 : cheio);
    }
    return valores;
  }

  /**
   * Recalcula as diárias e o {@code valor_total} de uma reserva a partir do conjunto ATUAL de
   * pessoas (todas as pessoas entram em todas as diárias, como na criação). Qualquer
   * desconto/ajuste manual vigente é REMOVIDO — ao mudar as pessoas o desconto deve ser reaplicado
   * manualmente. Atua somente sobre reservas (RESERVA_ATIVA / RESERVA_SOLICITADA); demais status
   * ficam intactos.
   */
  private void recalcularPorPessoas(Long hospedagemId) {
    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
    if (!EnumSet.of(Hospedagem.Status.RESERVA_ATIVA, Hospedagem.Status.RESERVA_SOLICITADA)
        .contains(hospedagem.status())) {
      return;
    }
    List<Hospedagem.Diaria> atuais = hospedagemRepository.listarDiarias(hospedagemId);
    if (atuais.isEmpty()) return;
    List<Long> pessoasIds = hospedagemRepository.buscarPessoasIds(hospedagemId);

    List<Hospedagem.Diaria.Request> reqs =
        atuais.stream()
            .map(
                d ->
                    new Hospedagem.Diaria.Request(
                        d.quarto() != null ? d.quarto().id() : null,
                        d.checkin(),
                        d.checkout(),
                        d.meia_diaria(),
                        pessoasIds))
            .toList();

    List<Double> valores = precificarDiarias(reqs);
    hospedagemRepository.deletarDiarias(hospedagemId);
    hospedagemRepository.adicionarDiarias(hospedagemId, reqs, valores);
    double total = valores.stream().mapToDouble(Double::doubleValue).sum();
    hospedagemRepository.atualizarValorTotal(hospedagemId, total);
    // Mudança de pessoas remove o desconto/ajuste manual vigente.
    hospedagemRepository.deletarNovoPreco(hospedagemId);

    log.info(
        "Recálculo por pessoas: hospedagem {} -> {} diárias, total {} (desconto removido)",
        hospedagemId,
        reqs.size(),
        total);
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
    Hospedagem hospedagem = buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), novoStatus);
    alterarStatus(hospedagemId, novoStatus);
    if (novoStatus == Hospedagem.Status.PERNOITE_ATIVO) {
      quartoRepository.updateStatus(hospedagem.quarto().id(), Quarto.Status.OCUPADO);
    }
  }

  // ── Pessoas ──────────────────────────────────────────────────────────────────
  public List<Pessoa.DadosPrincipais> buscarPessoasHospedagem(Long hospedagemId) {
    return pessoaService.buscarByHospedagemId(hospedagemId);
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

  /**
   * Adiciona pessoas a uma reserva (endpoint "Gerenciar Pessoas") e recalcula diárias/valor_total
   * conforme a nova ocupação, removendo qualquer desconto vigente. Usado pelo controller; os fluxos
   * internos de criação continuam usando {@link #adicionarPessoas} (sem recálculo).
   */
  @Transactional
  public void adicionarPessoasReserva(Long hospedagemId, List<Long> pessoasIds) {
    adicionarPessoas(hospedagemId, pessoasIds);
    recalcularPorPessoas(hospedagemId);
  }

  /** Remove pessoas de uma reserva e recalcula diárias/valor_total (remove desconto vigente). */
  @Transactional
  public void removerPessoasReserva(Long hospedagemId, List<Long> pessoasIds) {
    removerPessoas(hospedagemId, pessoasIds);
    recalcularPorPessoas(hospedagemId);
  }

  /**
   * Define outra pessoa como titular (representante) da hospedagem. A pessoa precisa já estar
   * vinculada. Não altera preço — apenas quem é o titular.
   */
  @Transactional
  public void definirTitular(Long hospedagemId, Long pessoaId) {
    List<Long> vinculadas = hospedagemRepository.buscarPessoasIds(hospedagemId);
    if (!vinculadas.contains(pessoaId)) {
      throw new IllegalArgumentException("Pessoa não está vinculada a esta hospedagem.");
    }
    hospedagemRepository.definirTitular(hospedagemId, pessoaId);
  }

  // ── Pagamentos ───────────────────────────────────────────────────────────────

  public void adicionarHospedagemPagamento(Long hospedagemId, List<UUID> pagamentosUUID) {
    hospedagemRepository.adicionarHospedagemPagamento(hospedagemId, pagamentosUUID);
  }

  public void adicionarHospedagemPagamento(
      Long hospedagemId, List<UUID> pagamentosUUID, Long grupoId) {
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
  public void adicionarPagamentoMultiplasHospedagens(
      List<Long> hospedagemIds, Pagamento.Request pagamento) {
    validarCamposPagamentoUnico(pagamento);
    var newPagamento = pagamentoService.criar(pagamento);
    List<UUID> pagamentosUUID = List.of(newPagamento.uuid());
    hospedagemIds.forEach(
        id -> {
          var hospedagem = hospedagemRepository.buscarPorId(id);
          adicionarHospedagemPagamento(id, pagamentosUUID);
          adicionarRelatorioHospedagem(
              hospedagem.quarto().id(),
              newPagamento,
              pagamento.arquivo(),
              hospedagem.status(),
              null);
        });
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
    adicionarRelatorioHospedagem(null, newPagamento, pagamento.arquivo(), null, grupoId);
    log.info(
        "Pagamento {} adicionado ao grupo {} (hospedagens {})",
        newPagamento.uuid(),
        grupoId,
        hospedagemIds);
  }

  /** Lista todos os grupos existentes (para vincular novas reservas a um grupo já criado). */
  public List<HospedagemRepository.GrupoInfo> listarGrupos() {
    return hospedagemRepository.listarGrupos();
  }

  /** Todas as hospedagens de um grupo, com detalhes — independentemente do mês/data. */
  public List<Hospedagem> buscarHospedagensGrupo(Long grupoId) {
    List<Long> ids = hospedagemRepository.buscarHospedagemIdsPorGrupo(grupoId);
    if (ids == null || ids.isEmpty()) return List.of();
    return withDetailsBatch(ids.stream().map(hospedagemRepository::buscarPorId).toList());
  }

  /** Totais consolidados de um grupo (todas as hospedagens, independentemente do status). */
  public record GrupoResumo(Long grupo_id, int count, double total, double pago, double pendente) {}

  public GrupoResumo buscarResumoGrupo(Long grupoId) {
    List<Long> ids = hospedagemRepository.buscarHospedagemIdsPorGrupo(grupoId);
    if (ids == null || ids.isEmpty()) return new GrupoResumo(grupoId, 0, 0.0, 0.0, 0.0);
    List<Hospedagem> membros =
        withDetailsBatch(ids.stream().map(hospedagemRepository::buscarPorId).toList());

    Set<Hospedagem.Status> cancelados =
        EnumSet.of(
            Hospedagem.Status.RESERVA_CANCELADA,
            Hospedagem.Status.PERNOITE_CANCELADO,
            Hospedagem.Status.DAY_USE_CANCELADO,
            Hospedagem.Status.ORCAMENTO_CANCELADO);

    double total = 0.0;
    double pago = 0.0;
    int considerados = 0;
    Set<UUID> pagamentosVistos = new HashSet<>();
    for (Hospedagem h : membros) {
      if (cancelados.contains(h.status())) continue; // hospedagens canceladas não entram no total
      considerados++;
      double consumoSum =
          h.consumos() == null
              ? 0.0
              : h.consumos().stream()
                  .filter(c -> !Boolean.TRUE.equals(c.cancelado()))
                  // valor é o preço unitário; o total da linha é valor * quantidade.
                  .mapToDouble(
                      c ->
                          (c.valor() == null ? 0.0 : c.valor())
                              * (c.quantidade() == null ? 0.0 : c.quantidade()))
                  .sum();
      total += (h.valor_total() == null ? 0.0 : h.valor_total()) + consumoSum;

      if (h.pagamentos() != null) {
        for (Pagamento p : h.pagamentos()) {
          if (Boolean.TRUE.equals(p.cancelado())) continue;
          if (!pagamentosVistos.add(p.uuid())) continue; // pagamento de grupo aparece em vários
          String tipo = p.tipo_pagamento() != null ? p.tipo_pagamento().descricao() : null;
          if (tipo == null || !tipo.equalsIgnoreCase("PENDENTE")) {
            pago += p.valor() == null ? 0.0 : p.valor();
          }
        }
      }
    }
    return new GrupoResumo(grupoId, considerados, total, pago, Math.max(0.0, total - pago));
  }

  @Transactional
  public void adicionarPagamentos(
      Long hospedagemId,
      Long quartoId,
      List<Pagamento.Request> requests,
      Hospedagem.Status status) {
    if (requests != null && !requests.isEmpty()) {
      List<UUID> pagamentosUUID = new ArrayList<>();
      requests.forEach(
          pagamento -> {
            var newPagamento = pagamentoService.criar(pagamento);
            pagamentosUUID.add(newPagamento.uuid());
            adicionarRelatorioHospedagem(quartoId, newPagamento, pagamento.arquivo(), status, null);
          });
      adicionarHospedagemPagamento(hospedagemId, pagamentosUUID);
    }
  }

  public void adicionarRelatorioHospedagem(
      Long quartoId,
      Pagamento pagamento,
      MultipartFile arquivo,
      Hospedagem.Status status,
      Long grupoId) {
    String relatorio = "";

    switch (status) {
      case RESERVA_ATIVA ->
          relatorio =
              "Pagamento de Reserva (" + pagamento.nome_pagador() + ") " + pagamento.descricao();
      case PERNOITE_ATIVO ->
          relatorio =
              "[Quarto "
                  + quartoId
                  + "] | PERNOITE | "
                  + pagamento.nome_pagador()
                  + " | "
                  + pagamento.descricao();
      case DAY_USE_ATIVO ->
          relatorio =
              "[Quarto "
                  + quartoId
                  + "] | DAY USE | "
                  + pagamento.nome_pagador()
                  + " | "
                  + pagamento.descricao();
      case null -> {
        return;
      }
      default -> throw new IllegalStateException("Unexpected value: " + status);
    }
    if (grupoId != null) {
      relatorio = relatorio + " | Grupo #" + grupoId;
    }

    Relatorio.Request relatorioRequest =
        new Relatorio.Request(
            relatorio,
            Relatorio.Registro.ENTRADA,
            false,
            new Pagamento.Request(
                new Pagamento.TipoPagamento.Id(pagamento.tipo_pagamento().id()),
                pagamento.nome_pagador(),
                relatorio,
                pagamento.valor(),
                arquivo),
            new Quarto.Id(quartoId));
    try {
      relatorioService.criar(relatorioRequest, arquivo);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.info("Adicionado Relatorio: [{}]", relatorioRequest);
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
                      null,
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
    LocalDateTime checkin = request.data_hora_checkin();
    LocalDateTime checkout = request.data_hora_checkout();
    // Hora de checkout (definida pela categoria) — usada como fim de cada diária para não carregar
    // a hora de check-in (ex.: 19:00) para o fim da diária e gerar conflito de quarto.
    LocalTime horaCheckout = checkout.toLocalTime();
    int total_diarias = Period.between(checkin.toLocalDate(), checkout.toLocalDate()).getDays();
    LocalDateTime inicioDiaria = checkin;
    for (int i = 0; i < total_diarias; i++) {
      // Cada diária encerra no dia seguinte, na hora de checkout; a próxima começa nesse mesmo
      // ponto.
      LocalDateTime fimDiaria =
          LocalDateTime.of(checkin.toLocalDate().plusDays(i + 1L), horaCheckout);
      diarias.add(
          new Hospedagem.Diaria.Request(
              request.quarto_id(), inicioDiaria, fimDiaria, false, request.pessoas()));
      inicioDiaria = fimDiaria;
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
      aplicarNovoPrecoCriacao(resolvedId, request.novo_preco());
      validarTransicaoDeStatus(statusAtual, Hospedagem.Status.RESERVA_ATIVA);

      if (request.pessoas() != null && !request.pessoas().isEmpty())
        adicionarPessoas(resolvedId, request.pessoas());

      if (!Boolean.TRUE.equals(pagamentoUnico)) {
        if (request.pagamentos() != null && !request.pagamentos().isEmpty()) {
          validarCamposPagamento(request);
          adicionarPagamentos(
              resolvedId, request.quarto_id(), request.pagamentos(), request.status());
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

    // Phase 2 — link reservations to a group. Quando o request carrega um grupo_id existente,
    // vincula a esse grupo; caso contrário, cria um novo grupo quando houver mais de uma reserva.
    Long grupoId = null;
    Long grupoExistente =
        requests.stream()
            .map(Hospedagem.Request::grupo_id)
            .filter(g -> g != null)
            .findFirst()
            .orElse(null);
    if (grupoExistente != null) {
      grupoId = grupoExistente;
      hospedagemRepository.vincularHospedagensGrupo(resolvedIds, grupoId);
      log.info("Hospedagens {} vinculadas ao grupo existente {}", resolvedIds, grupoId);
    } else if (resolvedIds.size() > 1) {
      grupoId = hospedagemRepository.criarGrupoReserva(getFuncionarioId());
      hospedagemRepository.vincularHospedagensGrupo(resolvedIds, grupoId);
      log.info("Grupo {} criado para as hospedagens {}", grupoId, resolvedIds);
    }

    // Phase 3 — create one payment and link it to every reservation (carrying the group when
    // present)
    if (Boolean.TRUE.equals(pagamentoUnico)) {
      Hospedagem.Request firstWithPagamentos =
          requests.stream()
              .filter(r -> r.pagamentos() != null && !r.pagamentos().isEmpty())
              .findFirst()
              .orElse(null);

      if (firstWithPagamentos != null) {
        validarCamposPagamento(firstWithPagamentos);
        List<UUID> pagamentosUUID = new ArrayList<>();
        firstWithPagamentos
            .pagamentos()
            .forEach(
                pagamento -> {
                  var newPagamento = pagamentoService.criar(pagamento);
                  pagamentosUUID.add(newPagamento.uuid());
                  adicionarRelatorioHospedagem(
                      firstWithPagamentos.quarto_id(),
                      newPagamento,
                      pagamento.arquivo(),
                      firstWithPagamentos.status(),
                      null);
                });
        final Long grupoIdFinal = grupoId;
        resolvedIds.forEach(id -> adicionarHospedagemPagamento(id, pagamentosUUID, grupoIdFinal));
      }
    }
  }

  /**
   * Cria um (ou vários) pernoite(s) diretamente, sem passar por reserva. Espelha {@link
   * #ativarReserva}, mas o status alvo é {@link Hospedagem.Status#PERNOITE_ATIVO} e o quarto passa
   * a OCUPADO. As requisições já chegam com {@code status = PERNOITE_ATIVO}.
   */
  @Transactional
  public void criarPernoiteDireto(List<Hospedagem.Request> requests, Boolean pagamentoUnico) {
    // Phase 1 — create every overnight (no payment when pagamentoUnico=true)
    List<Long> resolvedIds = new ArrayList<>();

    for (Hospedagem.Request request : requests) {
      validarCamposHospedagem(request, false);

      final Long resolvedId;
      final Hospedagem.Status statusAtual;
      Long hospedagemId = request.hospedagem_id();

      if (hospedagemId != null) {
        Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);
        if (hospedagem.status().equals(Hospedagem.Status.PERNOITE_ATIVO))
          throw new IllegalArgumentException("Pernoite já ativo para esse quarto e data.");
        resolvedId = hospedagemId;
        statusAtual = hospedagem.status();
      } else {
        resolvedId = hospedagemRepository.insertHospedagemId(request, getFuncionarioId());
        statusAtual = request.status();
      }
      calcularDiarias(resolvedId, request);
      aplicarNovoPrecoCriacao(resolvedId, request.novo_preco());
      validarTransicaoDeStatus(statusAtual, Hospedagem.Status.PERNOITE_ATIVO);

      if (request.pessoas() != null && !request.pessoas().isEmpty())
        adicionarPessoas(resolvedId, request.pessoas());

      if (!Boolean.TRUE.equals(pagamentoUnico)) {
        if (request.pagamentos() != null && !request.pagamentos().isEmpty()) {
          validarCamposPagamento(request);
          adicionarPagamentos(
              resolvedId, request.quarto_id(), request.pagamentos(), request.status());
        }
      }

      alterarStatus(resolvedId, Hospedagem.Status.PERNOITE_ATIVO);

      quartoRepository.updateStatus(request.quarto_id(), Quarto.Status.OCUPADO);

      resolvedIds.add(resolvedId);
    }

    // Phase 2 — link overnights to a group. Vincula a um grupo existente informado no request ou,
    // na ausência dele, cria um novo grupo quando houver mais de um pernoite.
    Long grupoId = null;
    Long grupoExistente =
        requests.stream()
            .map(Hospedagem.Request::grupo_id)
            .filter(g -> g != null)
            .findFirst()
            .orElse(null);
    if (grupoExistente != null) {
      grupoId = grupoExistente;
      hospedagemRepository.vincularHospedagensGrupo(resolvedIds, grupoId);
      log.info("Pernoites {} vinculados ao grupo existente {}", resolvedIds, grupoId);
    } else if (resolvedIds.size() > 1) {
      grupoId = hospedagemRepository.criarGrupoReserva(getFuncionarioId());
      hospedagemRepository.vincularHospedagensGrupo(resolvedIds, grupoId);
      log.info("Grupo {} criado para os pernoites {}", grupoId, resolvedIds);
    }

    // Phase 3 — create one payment and link it to every overnight (carrying the group when present)
    if (Boolean.TRUE.equals(pagamentoUnico)) {
      Hospedagem.Request firstWithPagamentos =
          requests.stream()
              .filter(r -> r.pagamentos() != null && !r.pagamentos().isEmpty())
              .findFirst()
              .orElse(null);

      if (firstWithPagamentos != null) {
        validarCamposPagamento(firstWithPagamentos);
        List<UUID> pagamentosUUID = new ArrayList<>();
        firstWithPagamentos
            .pagamentos()
            .forEach(
                pagamento -> {
                  var newPagamento = pagamentoService.criar(pagamento);
                  pagamentosUUID.add(newPagamento.uuid());
                  adicionarRelatorioHospedagem(
                      firstWithPagamentos.quarto_id(),
                      newPagamento,
                      pagamento.arquivo(),
                      firstWithPagamentos.status(),
                      null);
                });
        final Long grupoIdFinal = grupoId;
        resolvedIds.forEach(id -> adicionarHospedagemPagamento(id, pagamentosUUID, grupoIdFinal));
      }
    }
  }

  @Transactional
  public void cancelarReserva(Long hospedagemId, MotivoCancelamentoHospedagem.Request motivo) {
    Hospedagem hospedagem = buscarPorId(hospedagemId);
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
                false,
                request.pessoas()));
    adicionarDiarias(hospedagemId, diarias);
    adicionarPessoas(hospedagemId, request.pessoas());
    adicionarPagamentos(hospedagemId, request.quarto_id(), request.pagamentos(), request.status());
    alterarStatus(hospedagemId, Hospedagem.Status.PERNOITE_ATIVO);
  }

  @Transactional
  public void cancelarPernoite(Long hospedagemId, MotivoCancelamentoHospedagem.Request motivo) {
    Hospedagem hospedagem = buscarPorId(hospedagemId);
    validarTransicaoDeStatus(hospedagem.status(), Hospedagem.Status.PERNOITE_CANCELADO);
    adicionarMotivoCancelamento(motivo);
    alterarStatus(hospedagemId, Hospedagem.Status.PERNOITE_CANCELADO);
    quartoRepository.updateStatus(hospedagem.quarto().id(), Quarto.Status.DISPONIVEL);
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
    adicionarPagamentos(hospedagemId, request.quarto_id(), request.pagamentos(), request.status());
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

  /**
   * Aplica um ajuste manual de preço ("Gerenciar Preços") a uma hospedagem. O cálculo é feito no
   * front-end; aqui apenas persistimos: o snapshot do ajuste (com o funcionário responsável), o
   * {@code valor_total} resultante e, no modo "valor por diária", os novos valores das diárias.
   */
  @Transactional
  public Hospedagem gerenciarPreco(Long hospedagemId, HospedagemNovoPreco.Request request) {
    hospedagemRepository.buscarPorId(hospedagemId);

    hospedagemRepository.atualizarValoresDiarias(request.diarias());
    hospedagemRepository.salvarNovoPreco(hospedagemId, request, getFuncionarioId());
    if (request.valor_total() != null) {
      hospedagemRepository.atualizarValorTotal(hospedagemId, request.valor_total());
    }

    return buscarPorId(hospedagemId);
  }

  /**
   * Aplica o ajuste manual de preço no momento da criação da reserva. As diárias já foram criadas
   * por {@code calcularDiarias}; no modo "valor por diária" aplicamos o delta (com sinal) sobre
   * elas. O cálculo do total é feito no front-end e chega em {@code novo_preco.valor_total}.
   */
  private void aplicarNovoPrecoCriacao(Long hospedagemId, HospedagemNovoPreco.Request novoPreco) {
    if (novoPreco == null) return;
    if (novoPreco.valor_diaria() != null) {
      hospedagemRepository.sobrescreverValorDiarias(hospedagemId, novoPreco.valor_diaria());
    }
    hospedagemRepository.salvarNovoPreco(hospedagemId, novoPreco, getFuncionarioId());
    if (novoPreco.valor_total() != null) {
      hospedagemRepository.atualizarValorTotal(hospedagemId, novoPreco.valor_total());
    }
  }

  @Transactional
  public Hospedagem editarReserva(Long hospedagemId, Hospedagem.Request request) {
    if (hospedagemId == null) throw new IllegalArgumentException("Id da hospedagem é obrigatório.");

    Hospedagem hospedagem = hospedagemRepository.buscarPorId(hospedagemId);

    if (!EnumSet.of(Hospedagem.Status.RESERVA_SOLICITADA, Hospedagem.Status.RESERVA_ATIVA)
        .contains(hospedagem.status())) {
      throw new IllegalStateException(
          "Status " + hospedagem.status() + " não permite edição da reserva.");
    }

    if (request.data_hora_checkin() != null
        && request.data_hora_checkout() != null
        && !request.data_hora_checkin().isBefore(request.data_hora_checkout())) {
      throw new IllegalArgumentException("A data de checkin deve ser anterior à data de checkout.");
    }

    boolean alterouPeriodo =
        request.quarto_id() != null
            || request.data_hora_checkin() != null
            || request.data_hora_checkout() != null;

    if (alterouPeriodo) {
      Long quartoIdFinal =
          request.quarto_id() != null
              ? request.quarto_id()
              : hospedagemRepository.buscarQuartoId(hospedagemId);
      LocalDateTime checkinFinal =
          request.data_hora_checkin() != null
              ? request.data_hora_checkin()
              : hospedagem.data_hora_checkin();
      LocalDateTime checkoutFinal =
          request.data_hora_checkout() != null
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
                  null,
                  null,
                  null);
          calcularDiarias(hospedagemId, reqDiarias);
        }
      }
    }

    return withDetails(
        hospedagemRepository.editarReserva(hospedagemId, request, getFuncionarioId()));
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
  @Transactional(readOnly = true)
  public PageResult<Hospedagem> buscarPorQuarto(
      Long quartoId, Integer mes, Integer ano, String periodo, int page, int size) {
    long total = hospedagemRepository.contarPorQuarto(quartoId, mes, ano, periodo);
    List<Hospedagem> base =
        hospedagemRepository.buscarPorQuarto(quartoId, mes, ano, periodo, page, size);
    List<Hospedagem> content = base.isEmpty() ? List.of() : withDetailsBatch(base);
    int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    return new PageResult<>(content, page, size, total, totalPages);
  }

  private List<Hospedagem> withDetailsBatch(List<Hospedagem> hospedagens) {
    List<Long> ids = hospedagens.stream().map(Hospedagem::id).toList();

    Map<Long, List<Hospedagem.Diaria>> diariasMap = hospedagemRepository.listarDiariasBatch(ids);
    Map<Long, List<Item.Consumo>> consumosMap = hospedagemRepository.buscarConsumosBatch(ids);
    Map<Long, List<Pagamento>> pagamentosMap = pagamentoService.buscarPorHospedagemIds(ids);
    Map<Long, Quarto> quartosMap = quartoRepository.buscarPorHospedagemIds(ids);
    Map<Long, List<Pessoa.DadosPrincipais>> pessoasMap = pessoaService.buscarByHospedagemIds(ids);
    Map<Long, List<Hospedagem.PessoaHospedagemOrcamento>> pessoasOrcMap =
        hospedagemRepository.buscarPessoasOrcamentoBatch(ids);
    Map<Long, MotivoCancelamentoHospedagem> motivosMap =
        hospedagemRepository.buscarMotivoCancelamentoBatch(ids);
    Map<Long, HospedagemNovoPreco> novoPrecoMap = hospedagemRepository.buscarNovoPrecoBatch(ids);

    return hospedagens.stream()
        .map(
            h ->
                new Hospedagem(
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
                    h.grupo_id(),
                    novoPrecoMap.get(h.id())))
        .toList();
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
        hospedagem.grupo_id(),
        hospedagemRepository.buscarNovoPreco(hospedagem.id()));
  }

  public Map<Long, Hospedagem> buscarAtivasPorQuartoNaData(LocalDate data) {
    return hospedagemRepository.buscarAtivasPorQuartoNaData(data);
  }

  /**
   * Igual a {@link #buscarAtivasPorQuartoNaData}, porém com a hospedagem completa (diárias,
   * consumos, pagamentos, pessoas, pessoas de orçamento e motivo de cancelamento). Usa {@link
   * #withDetailsBatch} para evitar N+1.
   */
  public Map<Long, Hospedagem> buscarAtivasComDetalhesPorQuartoNaData(LocalDate data) {
    Map<Long, Hospedagem> base = hospedagemRepository.buscarAtivasPorQuartoNaData(data);
    if (base.isEmpty()) return Map.of();

    // Enriquece hospedagens distintas (uma hospedagem pode, em tese, mapear mais de um quarto).
    List<Hospedagem> distintas =
        base.values().stream()
            .collect(Collectors.toMap(Hospedagem::id, h -> h, (a, b) -> a))
            .values()
            .stream()
            .toList();
    Map<Long, Hospedagem> porId =
        withDetailsBatch(distintas).stream().collect(Collectors.toMap(Hospedagem::id, h -> h));

    Map<Long, Hospedagem> result = new LinkedHashMap<>();
    base.forEach((quartoId, h) -> result.put(quartoId, porId.getOrDefault(h.id(), h)));
    return result;
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
