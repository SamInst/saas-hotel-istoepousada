package saas.hotel.istoepousada.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Categoria;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.dto.Reserva;
import saas.hotel.istoepousada.dto.Sazonalidade;
import saas.hotel.istoepousada.handler.exceptions.BusinessException;
import saas.hotel.istoepousada.handler.exceptions.ConflictException;
import saas.hotel.istoepousada.repository.CategoriaRepository;
import saas.hotel.istoepousada.repository.PagamentoRepository;
import saas.hotel.istoepousada.repository.RelatorioRepository;
import saas.hotel.istoepousada.repository.ReservaRepository;

@Service
public class ReservaService {

  private final ReservaRepository reservaRepository;
  private final CategoriaRepository categoriaRepository;
  private final PagamentoRepository pagamentoRepository;
  private final RelatorioRepository relatorioRepository;

  public ReservaService(
      ReservaRepository reservaRepository,
      CategoriaRepository categoriaRepository,
      PagamentoRepository pagamentoRepository,
      RelatorioRepository relatorioRepository) {
    this.reservaRepository = reservaRepository;
    this.categoriaRepository = categoriaRepository;
    this.pagamentoRepository = pagamentoRepository;
    this.relatorioRepository = relatorioRepository;
  }

  // ── Consultas ──────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<Reserva.PorDia> buscarPorMesAno(int mes, int ano, Long id, String nome) {
    List<Reserva> reservas = reservaRepository.buscarPorMesAno(mes, ano, id, nome);
    return agruparPorDia(reservas);
  }

  @Transactional(readOnly = true)
  public List<Reserva> buscarPorData(LocalDate data, Long id, String nome) {
    if (data == null) throw new IllegalArgumentException("Data é obrigatória.");
    return reservaRepository.buscarPorData(data, id, nome);
  }

  @Transactional(readOnly = true)
  public List<Reserva> buscarPorQuartoMes(Long quartoId, int mes, int ano) {
    if (quartoId == null) throw new IllegalArgumentException("Id do quarto é obrigatório.");
    return reservaRepository.buscarPorQuartoMes(quartoId, mes, ano);
  }

  @Transactional(readOnly = true)
  public Reserva findById(Long id) {
    if (id == null) throw new IllegalArgumentException("Id é obrigatório.");
    return reservaRepository.findById(id);
  }

  // ── Inserção em lote ───────────────────────────────────────────────────────

  @Transactional
  public List<Reserva> inserirBatch(Reserva.BatchRequest request) {
    if (request == null || request.reservas() == null || request.reservas().isEmpty()) {
      throw new IllegalArgumentException("Lista de reservas é obrigatória.");
    }

    List<Reserva> resultados = new ArrayList<>();
    for (Reserva.Request req : request.reservas()) {
      resultados.add(inserirUma(req));
    }
    return resultados;
  }

  private Reserva inserirUma(Reserva.Request req) {
    validarRequest(req);

    ReservaRepository.CategoriaCheckin catInfo =
        reservaRepository.findCategoriaCheckinByQuartoId(req.fk_quarto());
    if (catInfo == null) {
      throw new BusinessException(
          "O quarto " + req.fk_quarto() + " não possui categoria configurada.");
    }

    LocalDateTime entrada = buildDateTime(req.data_entrada(), catInfo.hora_checkin());
    LocalDateTime saida = buildDateTime(req.data_saida(), catInfo.hora_checkout());

    if (!entrada.isBefore(saida)) {
      throw new BusinessException("Data de saída deve ser posterior à de entrada.");
    }

    if (reservaRepository.hasConflito(req.fk_quarto(), entrada, saida, null)) {
      throw new ConflictException(
          "O quarto " + req.fk_quarto() + " já possui reserva no período informado.");
    }

    double valorTotal = req.valor_total() != null ? req.valor_total() : 0.0;

    Long reservaId =
        reservaRepository.insertAndGetId(
            req.fk_quarto(), entrada, saida, valorTotal, req.observacao());

    if (req.pessoas() != null) {
      for (int i = 0; i < req.pessoas().size(); i++) {
        Reserva.PessoaRequest p = req.pessoas().get(i);
        reservaRepository.vincularPessoa(reservaId, p.fk_pessoa(), i == 0);
      }
    }

    if (req.pagamentos() != null) {
      for (Reserva.PagamentoReservaRequest pg : req.pagamentos()) {
        Pagamento criado =
            pagamentoRepository.create(
                new Pagamento.Request(
                    pg.tipo_pagamento(),
                    pg.nome_pagador(),
                    pg.descricao(),
                    pg.valor(),
                    null,
                    null));
        reservaRepository.vincularPagamento(reservaId, criado.uuid());
        relatorioRepository.registrarRelatorioDeConsumo(
            criado,
            relatorioRepository.getFuncionarioId(),
            "Reserva - Quarto " + req.fk_quarto());
      }
    }

    return reservaRepository.findById(reservaId);
  }

  // ── Edição ─────────────────────────────────────────────────────────────────

  @Transactional
  public Reserva atualizar(Reserva.Update update) {
    if (update == null || update.id() == null) {
      throw new IllegalArgumentException("Id da reserva é obrigatório.");
    }

    Reserva existing = reservaRepository.findById(update.id());

    Long novoQuartoId = update.fk_quarto() != null ? update.fk_quarto() : existing.quarto().id();

    LocalDateTime novaEntrada = null;
    LocalDateTime novaSaida = null;

    boolean mudouAlgo =
        update.fk_quarto() != null || update.data_entrada() != null || update.data_saida() != null;

    if (mudouAlgo) {
      ReservaRepository.CategoriaCheckin catInfo =
          reservaRepository.findCategoriaCheckinByQuartoId(novoQuartoId);
      if (catInfo == null) {
        throw new BusinessException(
            "O quarto " + novoQuartoId + " não possui categoria configurada.");
      }

      LocalDate dataEntrada =
          update.data_entrada() != null
              ? update.data_entrada()
              : existing.data_hora_entrada().toLocalDate();
      LocalDate dataSaida =
          update.data_saida() != null
              ? update.data_saida()
              : existing.data_hora_saida().toLocalDate();

      novaEntrada = buildDateTime(dataEntrada, catInfo.hora_checkin());
      novaSaida = buildDateTime(dataSaida, catInfo.hora_checkout());

      if (!novaEntrada.isBefore(novaSaida)) {
        throw new BusinessException("Data de saída deve ser posterior à de entrada.");
      }

      if (reservaRepository.hasConflito(novoQuartoId, novaEntrada, novaSaida, update.id())) {
        throw new ConflictException("O quarto já possui reserva no período informado.");
      }
    }

    return reservaRepository.update(
        update.id(), update.fk_quarto(), novaEntrada, novaSaida, update.valor_total(), update.observacao());
  }

  // ── Adição avulsa ─────────────────────────────────────────────────────────

  @Transactional
  public Reserva adicionarPessoa(Long reservaId, Reserva.PessoaRequest request) {
    if (reservaId == null) throw new IllegalArgumentException("Id da reserva é obrigatório.");
    if (request == null || request.fk_pessoa() == null)
      throw new IllegalArgumentException("Pessoa é obrigatória.");
    reservaRepository.findById(reservaId); // valida existência
    reservaRepository.vincularPessoa(
        reservaId, request.fk_pessoa(), Boolean.TRUE.equals(request.representante()));
    return reservaRepository.findById(reservaId);
  }

  @Transactional
  public Reserva adicionarPagamento(Long reservaId, Reserva.PagamentoReservaRequest request) {
    if (reservaId == null) throw new IllegalArgumentException("Id da reserva é obrigatório.");
    if (request == null) throw new IllegalArgumentException("Pagamento é obrigatório.");
    Reserva reserva = reservaRepository.findById(reservaId);
    Pagamento criado =
        pagamentoRepository.create(
            new Pagamento.Request(
                request.tipo_pagamento(),
                request.nome_pagador(),
                request.descricao(),
                request.valor(),
                null,
                null));
    reservaRepository.vincularPagamento(reservaId, criado.uuid());
    relatorioRepository.registrarRelatorioDeConsumo(
        criado,
        relatorioRepository.getFuncionarioId(),
        "Reserva - Quarto " + (reserva.quarto() != null ? reserva.quarto().descricao() : reservaId));
    return reservaRepository.findById(reservaId);
  }

  // ── Cancelamento ───────────────────────────────────────────────────────────

  @Transactional
  public void cancelar(Long id) {
    if (id == null) throw new IllegalArgumentException("Id é obrigatório.");
    reservaRepository.findById(id); // valida existência
    reservaRepository.cancelar(id);
  }

  // ── Cálculo de preços ─────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public Reserva.ResultadoPreco calcularPrecoPorPessoas(
      Long fkQuarto,
      LocalDate dataEntrada,
      LocalDate dataSaida,
      List<LocalDate> datasNascimento,
      LocalDateTime horaInicio,
      LocalDateTime horaFim) {

    if (fkQuarto == null) throw new IllegalArgumentException("fk_quarto é obrigatório.");
    if (datasNascimento == null || datasNascimento.isEmpty())
      throw new IllegalArgumentException("Informe ao menos uma data de nascimento.");

    // Referência de data para calcular idades: hora_inicio (day use) ou data_entrada (pernoite)
    LocalDate dataRef =
        horaInicio != null ? horaInicio.toLocalDate() : dataEntrada != null ? dataEntrada : null;
    if (dataRef == null) throw new IllegalArgumentException("Informe data_entrada ou hora_inicio.");

    List<Integer> idadesCriancas = new ArrayList<>();
    int qtdAdultos = 0;

    for (LocalDate nascimento : datasNascimento) {
      int idade = java.time.Period.between(nascimento, dataRef).getYears();
      if (idade >= 18) {
        qtdAdultos++;
      } else {
        idadesCriancas.add(idade);
      }
    }

    if (qtdAdultos == 0) {
      throw new BusinessException("É necessário ao menos um adulto (18 anos ou mais) na reserva.");
    }

    return calcularPrecoUnico(
        new Reserva.CalculoPrecosRequest(
            fkQuarto, dataEntrada, dataSaida, qtdAdultos, idadesCriancas, horaInicio, horaFim));
  }

  @Transactional(readOnly = true)
  public List<Reserva.ResultadoPreco> calcularPrecos(List<Reserva.CalculoPrecosRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      throw new IllegalArgumentException("Lista de solicitações é obrigatória.");
    }
    return requests.stream().map(this::calcularPrecoUnico).toList();
  }

  private Reserva.ResultadoPreco calcularPrecoUnico(Reserva.CalculoPrecosRequest req) {
    if (req.fk_quarto() == null) throw new IllegalArgumentException("fk_quarto é obrigatório.");
    if (req.quantidade_adultos() == null || req.quantidade_adultos() <= 0) {
      throw new IllegalArgumentException("quantidade_adultos deve ser maior que zero.");
    }

    // Day Use: quando hora_inicio e hora_fim são informados
    if (req.hora_inicio() != null && req.hora_fim() != null) {
      return calcularDayUseUnico(req);
    }

    if (req.data_entrada() == null)
      throw new IllegalArgumentException("data_entrada é obrigatória.");
    if (req.data_saida() == null) throw new IllegalArgumentException("data_saida é obrigatória.");

    if (req.data_entrada().isAfter(req.data_saida())) {
      throw new BusinessException("Data de saída não pode ser anterior à de entrada.");
    }

    // Datas iguais = entrada após meia-noite: calcula como 1 diária usando data_entrada - 1
    boolean mesmaData = req.data_entrada().isEqual(req.data_saida());
    LocalDate dataEntradaCalculo = mesmaData ? req.data_entrada().minusDays(1) : req.data_entrada();
    int noites =
        mesmaData ? 1 : (int) ChronoUnit.DAYS.between(req.data_entrada(), req.data_saida());

    ReservaRepository.CategoriaCheckin catInfo =
        reservaRepository.findCategoriaCheckinByQuartoId(req.fk_quarto());
    if (catInfo == null) {
      throw new BusinessException(
          "O quarto " + req.fk_quarto() + " não possui categoria configurada.");
    }

    Categoria categoria = categoriaRepository.findByIdOrThrow(catInfo.id());
    List<ReservaRepository.SazonInfo> sazonalidades =
        reservaRepository.findSazonalidades(catInfo.id());

    // Pré-carrega modelos próprios das sazonalidades (fk_categoria IS NULL)
    List<Long> sazonIds = sazonalidades.stream().map(ReservaRepository.SazonInfo::id).toList();
    Map<Long, List<Categoria.ModeloOcupacao>> sazonModelosOcupacao =
        reservaRepository.findSazonModelosOcupacao(sazonIds);
    Map<Long, List<Categoria.ModeloFixo>> sazonModelosFixo =
        reservaRepository.findSazonModelosFixo(sazonIds);
    Map<Long, List<Categoria.MenorIdade>> sazonMenoresIdade =
        categoriaRepository.findSazonMenoresIdade(sazonIds);

    String quartoDesc = reservaRepository.findQuartoDescricao(req.fk_quarto());
    Quarto.Descricao quartoObj = new Quarto.Descricao(req.fk_quarto(), quartoDesc);
    Categoria.Nome categoriaObj = new Categoria.Nome(catInfo.id(), catInfo.nome());

    double valorTotal = 0.0;
    List<Reserva.ItemPreco> detalhes = new ArrayList<>();
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Rastreia sazonalidades efetivamente aplicadas
    Map<Long, Sazonalidade.Nome> sazonAplicadasMap = new LinkedHashMap<>();

    boolean temCriancas = req.idades_criancas() != null && !req.idades_criancas().isEmpty();

    for (int i = 0; i < noites; i++) {
      LocalDate night = dataEntradaCalculo.plusDays(i);
      Long activeSazonId = findActiveSazonalidade(sazonalidades, night);

      // Registra sazonalidade detectada para incluir na resposta
      if (activeSazonId != null) {
        sazonAplicadasMap.computeIfAbsent(
            activeSazonId,
            id ->
                sazonalidades.stream()
                    .filter(s -> s.id().equals(id))
                    .findFirst()
                    .map(s -> new Sazonalidade.Nome(s.id(), s.descricao()))
                    .orElse(null));
      }

      // Prioridade 1: modelos próprios da sazonalidade (fk_categoria IS NULL)
      List<Categoria.ModeloOcupacao> modelosOcupacao =
          activeSazonId != null
              ? sazonModelosOcupacao.getOrDefault(activeSazonId, List.of())
              : List.of();
      List<Categoria.ModeloFixo> modelosFixo =
          activeSazonId != null
              ? sazonModelosFixo.getOrDefault(activeSazonId, List.of())
              : List.of();

      // Prioridade 2: modelos da categoria vinculados àquela sazonalidade
      if (modelosOcupacao.isEmpty() && modelosFixo.isEmpty() && activeSazonId != null) {
        modelosOcupacao = filtrarPorSazon(categoria.modelos_ocupacao(), activeSazonId);
        modelosFixo = filtrarFixoPorSazon(categoria.modelos_fixo(), activeSazonId);
      }

      // Prioridade 3: modelos base da categoria (sem sazonalidade)
      boolean usandoBase = modelosOcupacao.isEmpty() && modelosFixo.isEmpty();
      if (usandoBase) {
        modelosOcupacao = filtrarPorSazon(categoria.modelos_ocupacao(), null);
        modelosFixo = filtrarFixoPorSazon(categoria.modelos_fixo(), null);
      }

      // Preço base sempre calculado dos modelos sem sazonalidade (para o acréscimo)
      List<Categoria.ModeloOcupacao> baseOcupacao =
          filtrarPorSazon(categoria.modelos_ocupacao(), null);
      List<Categoria.ModeloFixo> baseFixo = filtrarFixoPorSazon(categoria.modelos_fixo(), null);
      double precoBase = resolverPrecoAdultos(baseOcupacao, baseFixo, req.quantidade_adultos());

      double noiteAdultosPreco =
          resolverPrecoAdultos(modelosOcupacao, modelosFixo, req.quantidade_adultos());
      String adultoLabel = req.quantidade_adultos() + " Adulto(s)";
      if (!modelosFixo.isEmpty() && modelosOcupacao.isEmpty()) adultoLabel = "tarifa fixa";

      // Crianças por noite
      double noiteCriancasPreco = 0.0;
      List<Integer> criancasComTaxa = new ArrayList<>();
      List<Integer> criancasGratuitas = new ArrayList<>();
      if (temCriancas) {
        // Prioridade 1: regra própria da sazonalidade (fk_categoria IS NULL)
        List<Categoria.MenorIdade> regras =
            activeSazonId != null
                ? sazonMenoresIdade.getOrDefault(activeSazonId, List.of())
                : List.of();
        // Prioridade 2: regra da categoria vinculada à sazonalidade
        if (regras.isEmpty() && activeSazonId != null)
          regras = filtrarMenoresPorSazon(categoria.menores_idade(), activeSazonId);
        // Prioridade 3: regra base da categoria
        if (regras.isEmpty()) regras = filtrarMenoresPorSazon(categoria.menores_idade(), null);
        if (!regras.isEmpty()) {
          Categoria.MenorIdade regra = regras.get(0);
          int qtdCriancas = req.idades_criancas().size();
          for (Integer idade : req.idades_criancas()) {
            double taxa =
                calcularTaxaCrianca(
                    regra, idade, req.quantidade_adultos(), noiteAdultosPreco, qtdCriancas);
            if (taxa > 0) {
              noiteCriancasPreco += taxa;
              criancasComTaxa.add(idade);
            } else {
              criancasGratuitas.add(idade);
            }
          }
        }
      }

      double acrescimo = usandoBase ? 0.0 : noiteAdultosPreco - precoBase;
      double noiteTotal = noiteAdultosPreco + noiteCriancasPreco;

      LocalDate nextNight = night.plusDays(1);
      StringBuilder desc =
          new StringBuilder(
              "Diaria "
                  + (i + 1)
                  + " - ("
                  + night.format(fmt)
                  + " -> "
                  + nextNight.format(fmt)
                  + ") "
                  + adultoLabel);
      if (!criancasComTaxa.isEmpty()) {
        if (criancasComTaxa.size() == 1) {
          desc.append(" + Criança de ").append(criancasComTaxa.get(0)).append(" anos");
        } else {
          desc.append(" + Crianças de ")
              .append(
                  criancasComTaxa.stream().map(String::valueOf).collect(Collectors.joining(", ")))
              .append(" anos");
        }
      }
      if (!criancasGratuitas.isEmpty()) {
        if (criancasGratuitas.size() == 1) {
          desc.append(" + Criança de ")
              .append(criancasGratuitas.get(0))
              .append(" anos (gratuidade)");
        } else {
          desc.append(" + Crianças de ")
              .append(
                  criancasGratuitas.stream().map(String::valueOf).collect(Collectors.joining(", ")))
              .append(" anos (gratuidade)");
        }
      }

      detalhes.add(
          new Reserva.ItemPreco(
              desc.toString(),
              precoBase,
              acrescimo,
              noiteCriancasPreco > 0 ? noiteCriancasPreco : null,
              noiteTotal));
      valorTotal += noiteTotal;
    }

    List<Sazonalidade.Nome> sazonAplicadas =
        sazonAplicadasMap.values().stream().filter(s -> s != null).toList();

    return new Reserva.ResultadoPreco(
        quartoObj,
        categoriaObj,
        req.data_entrada(),
        req.data_saida(),
        noites,
        valorTotal,
        sazonAplicadas.isEmpty() ? null : sazonAplicadas,
        detalhes);
  }

  // ── Cálculo Day Use ───────────────────────────────────────────────────────

  private Reserva.ResultadoPreco calcularDayUseUnico(Reserva.CalculoPrecosRequest req) {
    LocalDateTime horaInicio = req.hora_inicio();
    LocalDateTime horaFim = req.hora_fim();

    if (!horaInicio.isBefore(horaFim)) {
      throw new BusinessException("hora_fim deve ser posterior a hora_inicio.");
    }

    ReservaRepository.CategoriaCheckin catInfo =
        reservaRepository.findCategoriaCheckinByQuartoId(req.fk_quarto());
    if (catInfo == null) {
      throw new BusinessException(
          "O quarto " + req.fk_quarto() + " não possui categoria configurada.");
    }

    Categoria categoria = categoriaRepository.findByIdOrThrow(catInfo.id());
    List<ReservaRepository.SazonInfo> sazonalidades =
        reservaRepository.findSazonalidades(catInfo.id());

    LocalDate diaUso = horaInicio.toLocalDate();
    Long activeSazonId = findActiveSazonalidade(sazonalidades, diaUso);

    List<Long> sazonIds = sazonalidades.stream().map(ReservaRepository.SazonInfo::id).toList();
    Map<Long, Categoria.DayUseOperacao> sazonDayUse = reservaRepository.findSazonDayUse(sazonIds);

    // Base (sem sazonalidade) — sempre calculado para derivar o acréscimo
    Categoria.DayUseOperacao operacaoBase =
        categoria.day_use() == null
            ? null
            : categoria.day_use().stream()
                .filter(du -> du.sazonalidade() == null && Boolean.TRUE.equals(du.ativo()))
                .findFirst()
                .orElse(null);

    // Prioridade 1: Day Use próprio da sazonalidade
    Categoria.DayUseOperacao operacao =
        activeSazonId != null ? sazonDayUse.get(activeSazonId) : null;

    // Prioridade 2: Day Use da categoria vinculado à sazonalidade
    if (operacao == null && activeSazonId != null && categoria.day_use() != null) {
      Long sid = activeSazonId;
      operacao =
          categoria.day_use().stream()
              .filter(
                  du ->
                      du.sazonalidade() != null
                          && du.sazonalidade().id().equals(sid)
                          && Boolean.TRUE.equals(du.ativo()))
              .findFirst()
              .orElse(null);
    }

    // Prioridade 3: base
    boolean usandoBase = operacao == null;
    if (usandoBase) operacao = operacaoBase;

    if (operacao == null) {
      throw new BusinessException(
          "Nenhuma configuração de Day Use ativa encontrada para o quarto "
              + req.fk_quarto()
              + ".");
    }

    int totalPessoas =
        req.quantidade_adultos()
            + (req.idades_criancas() != null ? req.idades_criancas().size() : 0);
    double minutos = ChronoUnit.MINUTES.between(horaInicio, horaFim);
    double horas = minutos / 60.0;
    double horasPadrao =
        catInfo.hora_checkout() != null && catInfo.hora_checkin() != null
            ? ChronoUnit.MINUTES.between(catInfo.hora_checkin(), catInfo.hora_checkout()) / 60.0
            : horas;

    DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");
    DateTimeFormatter fmtDia = DateTimeFormatter.ofPattern("dd/MM");

    String periodoDesc =
        diaUso.format(fmtDia)
            + " "
            + horaInicio.format(fmtHora)
            + " - "
            + horaFim.format(fmtHora)
            + " ("
            + (int) Math.ceil(horas)
            + "h)";

    double valorTotal = calcularValorDayUse(operacao, horas, totalPessoas, horasPadrao);
    double valorBase =
        usandoBase || operacaoBase == null
            ? valorTotal
            : calcularValorDayUse(operacaoBase, horas, totalPessoas, horasPadrao);
    double acrescimo = valorTotal - valorBase;

    String tipoModelo = operacao.padrao() != null ? "padrão" : "por ocupação";
    String descricao =
        "Day Use " + periodoDesc + " - " + totalPessoas + " pessoa(s) - " + tipoModelo;

    String quartoDesc = reservaRepository.findQuartoDescricao(req.fk_quarto());
    Quarto.Descricao quartoObj = new Quarto.Descricao(req.fk_quarto(), quartoDesc);
    Categoria.Nome categoriaObj = new Categoria.Nome(catInfo.id(), catInfo.nome());

    Sazonalidade.Nome sazonNome =
        activeSazonId != null
            ? sazonalidades.stream()
                .filter(s -> s.id().equals(activeSazonId))
                .findFirst()
                .map(s -> new Sazonalidade.Nome(s.id(), s.descricao()))
                .orElse(null)
            : null;

    return new Reserva.ResultadoPreco(
        quartoObj,
        categoriaObj,
        horaInicio.toLocalDate(),
        horaFim.toLocalDate(),
        0,
        valorTotal,
        sazonNome != null ? List.of(sazonNome) : null,
        List.of(new Reserva.ItemPreco(descricao, valorBase, acrescimo, null, valorTotal)));
  }

  // ── Helpers de preço ──────────────────────────────────────────────────────

  private double resolverPrecoAdultos(
      List<Categoria.ModeloOcupacao> modelosOcupacao,
      List<Categoria.ModeloFixo> modelosFixo,
      int quantidade) {
    if (!modelosOcupacao.isEmpty()) {
      var modelo = modelosOcupacao.stream().filter(m -> m.quantidade() == quantidade).findFirst();
      if (modelo.isEmpty()) {
        modelo =
            modelosOcupacao.stream()
                .filter(m -> m.quantidade() <= quantidade)
                .max(Comparator.comparingInt(Categoria.ModeloOcupacao::quantidade));
      }
      if (modelo.isPresent()) return modelo.get().valor();
    } else if (!modelosFixo.isEmpty()) {
      return modelosFixo.get(0).valor();
    }
    return 0.0;
  }

  private double calcularValorDayUse(
      Categoria.DayUseOperacao operacao, double horas, int totalPessoas, double horasPadrao) {
    if (operacao.padrao() != null) {
      Categoria.DayUsePadrao padrao = operacao.padrao();
      double horasBase = padrao.hora_preco_base();
      if (horas <= horasBase) {
        return padrao.preco_base();
      }
      double horasExtras = Math.ceil(horas - horasBase);
      double valorExtra =
          padrao.valor_hora_adicional() != null ? padrao.valor_hora_adicional() : 0.0;
      return padrao.preco_base() + horasExtras * valorExtra;
    }
    if (operacao.ocupacoes() != null && !operacao.ocupacoes().isEmpty()) {
      Categoria.DayUseOcupacaoPessoa precoEncontrado = null;
      for (Categoria.DayUseOcupacao oc : operacao.ocupacoes()) {
        for (Categoria.DayUseOcupacaoPessoa p : oc.quantidades()) {
          if (p.quantidade() == totalPessoas) {
            precoEncontrado = p;
            break;
          }
          if (p.quantidade() <= totalPessoas
              && (precoEncontrado == null || p.quantidade() > precoEncontrado.quantidade())) {
            precoEncontrado = p;
          }
        }
        if (precoEncontrado != null && precoEncontrado.quantidade() == totalPessoas) break;
      }
      if (precoEncontrado == null) {
        throw new BusinessException(
            "Sem configuração de Day Use para " + totalPessoas + " pessoa(s).");
      }
      double valor = precoEncontrado.valor();
      if (precoEncontrado.valor_hora_adicional_por_pessoa() != null
          && precoEncontrado.valor_hora_adicional_por_pessoa() > 0
          && horas > horasPadrao) {
        double horasExtras = Math.ceil(horas - horasPadrao);
        valor += horasExtras * precoEncontrado.valor_hora_adicional_por_pessoa() * totalPessoas;
      }
      return valor;
    }
    return 0.0;
  }

  // ── Helpers de sazonalidade ────────────────────────────────────────────────

  private Long findActiveSazonalidade(
      List<ReservaRepository.SazonInfo> sazonalidades, LocalDate date) {

    // Sazonalidade de range de data é soberana: tem prioridade sobre qualquer outra.
    // Um período com dataInicio ou dataFim nulo representa range aberto (sem início ou sem fim).
    for (ReservaRepository.SazonInfo s : sazonalidades) {
      boolean isPeriodo = s.dataInicio() != null || s.dataFim() != null;
      if (!isPeriodo) continue;
      boolean inRange =
          (s.dataInicio() == null || !date.isBefore(s.dataInicio()))
              && (s.dataFim() == null || !date.isAfter(s.dataFim()));
      if (inRange) return s.id();
    }

    // Sem range de data: aplica semanal, mensal ou anual (primeira que bater)
    for (ReservaRepository.SazonInfo s : sazonalidades) {
      boolean appliesSemanal =
          s.semanal() != null
              && !s.semanal().isEmpty()
              && s.semanal().contains(date.getDayOfWeek().getValue());

      boolean appliesMensal =
          s.mensal() != null && !s.mensal().isEmpty() && s.mensal().contains(date.getDayOfMonth());

      boolean appliesAnual =
          s.anual() != null && !s.anual().isEmpty() && s.anual().contains(date.getMonthValue());

      if (appliesSemanal || appliesMensal || appliesAnual) {
        return s.id();
      }
    }

    return null;
  }

  private List<Categoria.ModeloOcupacao> filtrarPorSazon(
      List<Categoria.ModeloOcupacao> modelos, Long sazonId) {
    if (modelos == null) return List.of();
    return modelos.stream().filter(m -> sazonIdMatch(m.sazonalidade(), sazonId)).toList();
  }

  private List<Categoria.ModeloFixo> filtrarFixoPorSazon(
      List<Categoria.ModeloFixo> modelos, Long sazonId) {
    if (modelos == null) return List.of();
    return modelos.stream().filter(m -> sazonIdMatch(m.sazonalidade(), sazonId)).toList();
  }

  private List<Categoria.MenorIdade> filtrarMenoresPorSazon(
      List<Categoria.MenorIdade> modelos, Long sazonId) {
    if (modelos == null) return List.of();
    return modelos.stream().filter(m -> sazonIdMatch(m.sazonalidade(), sazonId)).toList();
  }

  private boolean sazonIdMatch(Sazonalidade.Nome sazon, Long activeSazonId) {
    if (activeSazonId == null) return sazon == null;
    return sazon != null && sazon.id().equals(activeSazonId);
  }

  // ── Cálculo de taxa de criança ─────────────────────────────────────────────

  private double calcularTaxaCrianca(
      Categoria.MenorIdade regra, int idade, int qtdAdultos, double valorBase, int qtdCriancas) {

    if (regra.idade_gratuidade() != null && idade <= regra.idade_gratuidade()) {
      return 0.0;
    }

    return switch (regra.modelo()) {
      case TAXA_ADICIONAL_FIXA -> {
        if (regra.taxas_fixas() == null) yield 0.0;
        yield regra.taxas_fixas().stream()
            .filter(t -> idade <= t.idade_maxima())
            .min(Comparator.comparingInt(Categoria.MenorTaxaFixa::idade_maxima))
            .map(Categoria.MenorTaxaFixa::valor_por_crianca)
            .orElse(0.0);
      }
      case TAXA_POR_QUANTIDADE -> {
        if (regra.taxas_por_quantidade() == null) yield 0.0;
        var taxa =
            regra.taxas_por_quantidade().stream()
                .filter(t -> t.quantidade_crianca() == qtdCriancas)
                .findFirst();
        if (taxa.isEmpty()) {
          taxa =
              regra.taxas_por_quantidade().stream()
                  .filter(t -> t.quantidade_crianca() <= qtdCriancas)
                  .max(
                      Comparator.comparingInt(
                          Categoria.MenorTaxaPorQuantidade::quantidade_crianca));
        }
        yield taxa.map(Categoria.MenorTaxaPorQuantidade::valor).orElse(0.0);
      }
      case TAXA_POR_FAIXA_ETARIA -> {
        if (regra.faixas_etarias() == null) yield 0.0;
        yield regra.faixas_etarias().stream()
            .filter(
                f ->
                    f.faixa_etaria() != null
                        && f.faixa_etaria().size() >= 2
                        && idade >= f.faixa_etaria().getFirst()
                        && idade <= f.faixa_etaria().get(1))
            .findFirst()
            .map(Categoria.MenorFaixaEtaria::valor)
            .orElse(0.0);
      }
      case PORCENTAGEM_POR_QUANTIDADE -> {
        if (regra.porcentagens_por_quantidade() == null) yield 0.0;
        var porc =
            regra.porcentagens_por_quantidade().stream()
                .filter(p -> p.quantidade() == qtdCriancas)
                .findFirst();
        if (porc.isEmpty()) {
          porc =
              regra.porcentagens_por_quantidade().stream()
                  .filter(p -> p.quantidade() <= qtdCriancas)
                  .max(
                      Comparator.comparingInt(Categoria.MenorPorcentagemPorQuantidade::quantidade));
        }
        double porcentagem = porc.map(p -> (double) p.porcentagem()).orElse(0.0);
        yield valorBase * porcentagem / 100.0;
      }
    };
  }

  // ── Agrupamento por dia ────────────────────────────────────────────────────

  private List<Reserva.PorDia> agruparPorDia(List<Reserva> reservas) {
    Map<LocalDate, List<Reserva>> porDia = new LinkedHashMap<>();
    for (Reserva r : reservas) {
      LocalDate dia =
          r.data_hora_entrada() != null ? r.data_hora_entrada().toLocalDate() : LocalDate.MIN;
      porDia.computeIfAbsent(dia, k -> new ArrayList<>()).add(r);
    }
    return porDia.entrySet().stream()
        .map(e -> new Reserva.PorDia(e.getKey(), e.getValue()))
        .toList();
  }

  // ── Validações ─────────────────────────────────────────────────────────────

  private void validarRequest(Reserva.Request req) {
    if (req == null) throw new IllegalArgumentException("Dados da reserva são obrigatórios.");
    if (req.fk_quarto() == null) throw new IllegalArgumentException("Quarto é obrigatório.");
    if (req.data_entrada() == null)
      throw new IllegalArgumentException("Data de entrada é obrigatória.");
    if (req.data_saida() == null)
      throw new IllegalArgumentException("Data de saída é obrigatória.");
    if (!req.data_entrada().isBefore(req.data_saida())) {
      throw new IllegalArgumentException("Data de saída deve ser posterior à de entrada.");
    }
  }

  private LocalDateTime buildDateTime(LocalDate date, LocalTime time) {
    return LocalDateTime.of(date, time != null ? time : LocalTime.MIDNIGHT);
  }
}
