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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Categoria;
import saas.hotel.istoepousada.dto.Reserva;
import saas.hotel.istoepousada.dto.Sazonalidade;
import saas.hotel.istoepousada.handler.exceptions.BusinessException;
import saas.hotel.istoepousada.handler.exceptions.ConflictException;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.repository.CategoriaRepository;
import saas.hotel.istoepousada.repository.PagamentoRepository;
import saas.hotel.istoepousada.repository.ReservaRepository;

@Service
public class ReservaService {

  private final ReservaRepository reservaRepository;
  private final CategoriaRepository categoriaRepository;
  private final PagamentoRepository pagamentoRepository;

  public ReservaService(
      ReservaRepository reservaRepository,
      CategoriaRepository categoriaRepository,
      PagamentoRepository pagamentoRepository) {
    this.reservaRepository = reservaRepository;
    this.categoriaRepository = categoriaRepository;
    this.pagamentoRepository = pagamentoRepository;
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

    Long reservaId = reservaRepository.insertAndGetId(req.fk_quarto(), entrada, saida);

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
                    pg.tipo_pagamento(), pg.nome_pagador(), pg.descricao(), pg.valor(), null, null));
        reservaRepository.vincularPagamento(reservaId, criado.uuid());
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

    return reservaRepository.update(update.id(), update.fk_quarto(), novaEntrada, novaSaida);
  }

  // ── Adição avulsa ─────────────────────────────────────────────────────────

  @Transactional
  public Reserva adicionarPessoa(Long reservaId, Reserva.PessoaRequest request) {
    if (reservaId == null) throw new IllegalArgumentException("Id da reserva é obrigatório.");
    if (request == null || request.fk_pessoa() == null)
      throw new IllegalArgumentException("Pessoa é obrigatória.");
    reservaRepository.findById(reservaId); // valida existência
    reservaRepository.vincularPessoa(reservaId, request.fk_pessoa(), Boolean.TRUE.equals(request.representante()));
    return reservaRepository.findById(reservaId);
  }

  @Transactional
  public Reserva adicionarPagamento(Long reservaId, Reserva.PagamentoReservaRequest request) {
    if (reservaId == null) throw new IllegalArgumentException("Id da reserva é obrigatório.");
    if (request == null) throw new IllegalArgumentException("Pagamento é obrigatório.");
    reservaRepository.findById(reservaId); // valida existência
    Pagamento criado =
        pagamentoRepository.create(
            new Pagamento.Request(
                request.tipo_pagamento(), request.nome_pagador(), request.descricao(), request.valor(), null, null));
    reservaRepository.vincularPagamento(reservaId, criado.uuid());
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
  public List<Reserva.ResultadoPreco> calcularPrecos(List<Reserva.CalculoPrecosRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      throw new IllegalArgumentException("Lista de solicitações é obrigatória.");
    }
    return requests.stream().map(this::calcularPrecoUnico).toList();
  }

  private Reserva.ResultadoPreco calcularPrecoUnico(Reserva.CalculoPrecosRequest req) {
    if (req.fk_quarto() == null) throw new IllegalArgumentException("fk_quarto é obrigatório.");
    if (req.data_entrada() == null)
      throw new IllegalArgumentException("data_entrada é obrigatória.");
    if (req.data_saida() == null) throw new IllegalArgumentException("data_saida é obrigatória.");
    if (req.quantidade_adultos() == null || req.quantidade_adultos() <= 0) {
      throw new IllegalArgumentException("quantidade_adultos deve ser maior que zero.");
    }

    int noites = (int) ChronoUnit.DAYS.between(req.data_entrada(), req.data_saida());
    if (noites <= 0) {
      throw new BusinessException("Data de saída deve ser posterior à de entrada.");
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

    double valorBase = 0.0;
    List<Reserva.ItemPreco> detalhes = new ArrayList<>();
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

    for (int i = 0; i < noites; i++) {
      LocalDate night = req.data_entrada().plusDays(i);
      Long activeSazonId = findActiveSazonalidade(sazonalidades, night);

      List<Categoria.ModeloOcupacao> modelosOcupacao =
          filtrarPorSazon(categoria.modelos_ocupacao(), activeSazonId);
      List<Categoria.ModeloFixo> modelosFixo =
          filtrarFixoPorSazon(categoria.modelos_fixo(), activeSazonId);

      double noitePreco = 0.0;
      String diaDesc = "Noite " + night.format(fmt);

      if (!modelosOcupacao.isEmpty()) {
        var modelo =
            modelosOcupacao.stream()
                .filter(m -> m.quantidade().equals(req.quantidade_adultos()))
                .findFirst();
        if (modelo.isEmpty()) {
          modelo =
              modelosOcupacao.stream()
                  .filter(m -> m.quantidade() <= req.quantidade_adultos())
                  .max(Comparator.comparingInt(Categoria.ModeloOcupacao::quantidade));
        }
        if (modelo.isPresent()) {
          noitePreco = modelo.get().valor();
          detalhes.add(
              new Reserva.ItemPreco(
                  diaDesc + " - " + req.quantidade_adultos() + " adulto(s)", noitePreco));
        }
      } else if (!modelosFixo.isEmpty()) {
        noitePreco = modelosFixo.get(0).valor();
        detalhes.add(new Reserva.ItemPreco(diaDesc + " - preço fixo", noitePreco));
      }

      valorBase += noitePreco;
    }

    // Crianças (aplicado sobre o total de noites)
    double valorCriancas = 0.0;
    if (req.idades_criancas() != null && !req.idades_criancas().isEmpty()) {
      Long activeSazonPrimeiraDiaria = findActiveSazonalidade(sazonalidades, req.data_entrada());
      List<Categoria.MenorIdade> regras =
          filtrarMenoresPorSazon(categoria.menores_idade(), activeSazonPrimeiraDiaria);

      if (!regras.isEmpty()) {
        Categoria.MenorIdade regra = regras.get(0);
        int qtdCriancas = req.idades_criancas().size();
        for (Integer idade : req.idades_criancas()) {
          double taxaCrianca =
              calcularTaxaCrianca(regra, idade, req.quantidade_adultos(), valorBase, qtdCriancas);
          if (taxaCrianca > 0) {
            valorCriancas += taxaCrianca;
            detalhes.add(new Reserva.ItemPreco("Criança " + idade + " anos", taxaCrianca));
          }
        }
      }
    }

    String quartoDesc = reservaRepository.findQuartoDescricao(req.fk_quarto());

    return new Reserva.ResultadoPreco(
        req.fk_quarto(),
        quartoDesc,
        catInfo.id(),
        catInfo.nome(),
        req.data_entrada(),
        req.data_saida(),
        noites,
        valorBase,
        valorCriancas,
        valorBase + valorCriancas,
        detalhes);
  }

  // ── Helpers de sazonalidade ────────────────────────────────────────────────

  private Long findActiveSazonalidade(
      List<ReservaRepository.SazonInfo> sazonalidades, LocalDate date) {
    for (ReservaRepository.SazonInfo s : sazonalidades) {
      boolean appliesDateRange =
          s.dataInicio() != null
              && s.dataFim() != null
              && !date.isBefore(s.dataInicio())
              && !date.isAfter(s.dataFim());

      boolean appliesSemanal =
          s.semanal() != null
              && !s.semanal().isEmpty()
              && s.semanal().contains(date.getDayOfWeek().getValue() % 7);

      boolean appliesMensal =
          s.mensal() != null && !s.mensal().isEmpty() && s.mensal().contains(date.getDayOfMonth());

      boolean appliesAnual =
          s.anual() != null && !s.anual().isEmpty() && s.anual().contains(date.getDayOfYear());

      if (appliesDateRange || appliesSemanal || appliesMensal || appliesAnual) {
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
                        && idade >= f.faixa_etaria().get(0)
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
