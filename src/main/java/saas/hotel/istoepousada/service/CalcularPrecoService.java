package saas.hotel.istoepousada.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.*;
import saas.hotel.istoepousada.handler.exceptions.BusinessException;
import saas.hotel.istoepousada.repository.CategoriaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalcularPrecoService {
    private final CategoriaRepository categoriaRepository;
    private final QuartoService quartoService;

    public CalcularPrecoService(CategoriaRepository categoriaRepository, QuartoService quartoService) {
        this.categoriaRepository = categoriaRepository;
        this.quartoService = quartoService;
    }

    @Transactional(readOnly = true)
    public List<CalcularPreco.Resultado> calcularPreco(List<CalcularPreco.Request> requests) {
        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("Lista vazia.");

        List<CalcularPreco> resolved =
                requests.stream()
                        .map(this::calcularPorDataNascimento)
                        .toList();

        List<Long> quartoIds = resolved.stream().map(CalcularPreco::fk_quarto).toList();
        Map<Long, CategoriaCheckin> catInfoMap =
                categoriaRepository.findCategoriasCheckinByQuartoIds(quartoIds);

        for (CalcularPreco req : resolved) {
            if (catInfoMap.get(req.fk_quarto()) == null)
                throw new BusinessException(
                        "O quarto " + req.fk_quarto() + " não possui categoria configurada.");
        }

        List<Long> categoriaIds =
                catInfoMap.values().stream()
                        .map(CategoriaCheckin::id)
                        .distinct()
                        .toList();

        Map<Long, Categoria> categoriasMap =
                categoriaRepository.findCategoriasParaCalculo(categoriaIds);

        Map<Long, List<Sazonalidade>> sazonalidadesPorCategoriaId =
                categoriaRepository.findSazonalidades(categoriaIds);

        List<Long> sazonalidadeIds =
                sazonalidadesPorCategoriaId.values().stream()
                        .flatMap(List::stream)
                        .map(Sazonalidade::id)
                        .distinct()
                        .toList();

        Map<Long, List<Categoria.ModeloOcupacao>> sazonalidadeModeloPrecoPorOcupacao = sazonalidadeIds.isEmpty() ?
                Map.of() : categoriaRepository.buscaModeloPrecoPorOcupacaoSazonalidade(sazonalidadeIds);

        Map<Long, List<Categoria.ModeloFixo>> sazonalidadeModeloPrecoFixo = sazonalidadeIds.isEmpty() ?
                Map.of() : categoriaRepository.buscaModeloPrecoFixoSazonalidade(sazonalidadeIds);

        boolean temMenoresIdade =
                resolved.stream()
                        .anyMatch(r -> r.idades_criancas() != null && !r.idades_criancas().isEmpty());

        Map<Long, List<Categoria.MenorIdade>> sazonalidadeMenoresIdade =
                (temMenoresIdade && !sazonalidadeIds.isEmpty())
                        ? categoriaRepository.findSazonMenoresIdade(sazonalidadeIds)
                        : Map.of();

        boolean temDayUse = resolved.stream()
                .anyMatch(r ->
                        r.hora_inicio() != null
                                && r.hora_fim() != null);

        Map<Long, Categoria.DayUseOperacao> sazonDayUse = (temDayUse && !sazonalidadeIds.isEmpty()) ?
                categoriaRepository.buscaDayUseSazonalidade(sazonalidadeIds) : Map.of();

        Map<Long, String> quartosDescricao = quartoService.findQuartosDescricao(quartoIds);

        return resolved.stream()
                .map(
                        req -> {
                            CategoriaCheckin catInfo = catInfoMap.get(req.fk_quarto());
                            return calcularPrecoUnico(
                                    req,
                                    catInfo,
                                    categoriasMap.get(catInfo.id()),
                                    sazonalidadesPorCategoriaId.getOrDefault(catInfo.id(), List.of()),
                                    sazonalidadeModeloPrecoPorOcupacao,
                                    sazonalidadeModeloPrecoFixo,
                                    sazonalidadeMenoresIdade,
                                    sazonDayUse,
                                    quartosDescricao.get(req.fk_quarto()));
                        })
                .toList();
    }

    private CalcularPreco calcularPorDataNascimento(CalcularPreco.Request request) {
        if (request.datas_nascimento() == null || request.datas_nascimento().isEmpty())
            throw new IllegalArgumentException("Informe ao menos uma data de nascimento.");

        LocalDate dataRef =
                request.hora_inicio() != null ? request.hora_inicio().toLocalDate() : request.data_entrada();
        if (dataRef == null) throw new IllegalArgumentException("Informe data_entrada ou hora_inicio.");

        List<Integer> criancas = new ArrayList<>();
        int adultos = 0;
        for (LocalDate nascimento : request.datas_nascimento()) {
            int idade = java.time.Period.between(nascimento, dataRef).getYears();
            if (idade >= 18) adultos++;
            else criancas.add(idade);
        }
        if (adultos == 0)
            throw new BusinessException("É necessário ao menos um adulto (18 anos ou mais) na ");

        return new CalcularPreco(
                request.fk_quarto(),
                request.data_entrada(),
                request.data_saida(),
                adultos,
                criancas,
                request.hora_inicio(),
                request.hora_fim());
    }

    private CalcularPreco.Resultado calcularPrecoUnico(
            CalcularPreco request,
            CategoriaCheckin categoriaCheckin,
            Categoria categoria,
            List<Sazonalidade> sazonalidades,
            Map<Long, List<Categoria.ModeloOcupacao>> sazonalidadeModelosPrecoPorOcupacao,
            Map<Long, List<Categoria.ModeloFixo>> sazonalidadeModelosPrecoFixo,
            Map<Long, List<Categoria.MenorIdade>> sazonalidadeMenoresIdade,
            Map<Long, Categoria.DayUseOperacao> sazonalidadeDayUse,
            String quartoDesc) {
        if (request.fk_quarto() == null) throw new IllegalArgumentException("fk_quarto é obrigatório.");
        if (request.quantidade_adultos() == null || request.quantidade_adultos() <= 0) {
            throw new IllegalArgumentException("quantidade_adultos deve ser maior que zero.");
        }

        // Day Use: quando hora_inicio e hora_fim são informados
        if (request.hora_inicio() != null && request.hora_fim() != null) {
            return calcularDayUseUnico(request, categoriaCheckin, categoria, sazonalidades, sazonalidadeDayUse, quartoDesc);
        }

        if (request.data_entrada() == null)
            throw new IllegalArgumentException("data_entrada é obrigatória.");
        if (request.data_saida() == null) throw new IllegalArgumentException("data_saida é obrigatória.");

        if (request.data_entrada().isAfter(request.data_saida())) {
            throw new BusinessException("Data de saída não pode ser anterior à de entrada.");
        }

        // Datas iguais = entrada após meia-noite: calcula como 1 diária usando data_entrada - 1
        boolean mesmaData = request.data_entrada().isEqual(request.data_saida());
        LocalDate dataEntradaCalculo = mesmaData ? request.data_entrada().minusDays(1) : request.data_entrada();
        int noites =
                mesmaData ? 1 : (int) ChronoUnit.DAYS.between(request.data_entrada(), request.data_saida());

        Quarto.Descricao quartoObj = new Quarto.Descricao(request.fk_quarto(), quartoDesc);
        Categoria.Nome categoriaObj = new Categoria.Nome(categoriaCheckin.id(), categoriaCheckin.nome());

        double valorTotal = 0.0;
        List<CalcularPreco.ItemPreco> detalhes = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Rastreia sazonalidades efetivamente aplicadas
        Map<Long, Sazonalidade.Nome> sazonAplicadasMap = new LinkedHashMap<>();

        boolean temCriancas = request.idades_criancas() != null && !request.idades_criancas().isEmpty();

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
                            ? sazonalidadeModelosPrecoPorOcupacao.getOrDefault(activeSazonId, List.of())
                            : List.of();
            List<Categoria.ModeloFixo> modelosFixo =
                    activeSazonId != null
                            ? sazonalidadeModelosPrecoFixo.getOrDefault(activeSazonId, List.of())
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
            double precoBase = resolverPrecoAdultos(baseOcupacao, baseFixo, request.quantidade_adultos());

            double noiteAdultosPreco =
                    resolverPrecoAdultos(modelosOcupacao, modelosFixo, request.quantidade_adultos());
            String adultoLabel = request.quantidade_adultos() + " Adulto(s)";
            if (!modelosFixo.isEmpty() && modelosOcupacao.isEmpty()) adultoLabel = "tarifa fixa";

            // Crianças por noite
            double noiteCriancasPreco = 0.0;
            List<Integer> criancasComTaxa = new ArrayList<>();
            List<Integer> criancasGratuitas = new ArrayList<>();
            if (temCriancas) {
                // Prioridade 1: regra própria da sazonalidade (fk_categoria IS NULL)
                List<Categoria.MenorIdade> regras =
                        activeSazonId != null
                                ? sazonalidadeMenoresIdade.getOrDefault(activeSazonId, List.of())
                                : List.of();
                // Prioridade 2: regra da categoria vinculada à sazonalidade
                if (regras.isEmpty() && activeSazonId != null)
                    regras = filtrarMenoresPorSazon(categoria.menores_idade(), activeSazonId);
                // Prioridade 3: regra base da categoria
                if (regras.isEmpty()) regras = filtrarMenoresPorSazon(categoria.menores_idade(), null);
                if (!regras.isEmpty()) {
                    Categoria.MenorIdade regra = regras.getFirst();
                    int qtdCriancas = request.idades_criancas().size();
                    for (Integer idade : request.idades_criancas()) {
                        double taxa =
                                calcularTaxaCrianca(
                                        regra, idade, request.quantidade_adultos(), qtdCriancas);
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
                    desc.append(" + Criança de ").append(criancasComTaxa.getFirst()).append(" anos");
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
                            .append(criancasGratuitas.getFirst())
                            .append(" anos (gratuidade)");
                } else {
                    desc.append(" + Crianças de ")
                            .append(
                                    criancasGratuitas.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                            .append(" anos (gratuidade)");
                }
            }

            Sazonalidade.Nome sazonNomeItem =
                    activeSazonId != null ? sazonAplicadasMap.get(activeSazonId) : null;

            detalhes.add(
                    new CalcularPreco.ItemPreco(
                            desc.toString(),
                            sazonNomeItem,
                            precoBase,
                            acrescimo,
                            noiteCriancasPreco > 0 ? noiteCriancasPreco : null,
                            noiteTotal));
            valorTotal += noiteTotal;
        }

        List<Sazonalidade.Nome> sazonAplicadas =
                sazonAplicadasMap.values().stream().filter(Objects::nonNull).toList();

        return new CalcularPreco.Resultado(
                quartoObj,
                categoriaObj,
                request.data_entrada(),
                request.data_saida(),
                noites,
                valorTotal,
                sazonAplicadas.isEmpty() ? null : sazonAplicadas,
                detalhes);
    }

    // ── Cálculo Day Use ───────────────────────────────────────────────────────

    private CalcularPreco.Resultado calcularDayUseUnico(
            CalcularPreco req,
            CategoriaCheckin catInfo,
            Categoria categoria,
            List<Sazonalidade> sazonalidades,
            Map<Long, Categoria.DayUseOperacao> sazonDayUse,
            String quartoDesc) {
        LocalDateTime horaInicio = req.hora_inicio();
        LocalDateTime horaFim = req.hora_fim();

        if (!horaInicio.isBefore(horaFim)) {
            throw new BusinessException("hora_fim deve ser posterior a hora_inicio.");
        }

        LocalDate diaUso = horaInicio.toLocalDate();
        Long activeSazonId = findActiveSazonalidade(sazonalidades, diaUso);

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

        return new CalcularPreco.Resultado(
                quartoObj,
                categoriaObj,
                horaInicio.toLocalDate(),
                horaFim.toLocalDate(),
                0,
                valorTotal,
                sazonNome != null ? List.of(sazonNome) : null,
                List.of(
                        new CalcularPreco.ItemPreco(descricao, sazonNome, valorBase, acrescimo, null, valorTotal)));
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
            return modelosFixo.getFirst().valor();
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

    private Long findActiveSazonalidade(
            List<Sazonalidade> sazonalidades, LocalDate date) {

        // Sazonalidade de range de data é soberana: tem prioridade sobre qualquer outra.
        // Um período com dataInicio ou dataFim nulo representa range aberto (sem início ou sem fim).
        for (Sazonalidade s : sazonalidades) {
            boolean isPeriodo = s.data_inicio() != null || s.data_fim() != null;
            if (!isPeriodo) continue;
            boolean inRange =
                    (s.data_fim() == null || !date.isBefore(s.data_inicio()))
                            && (s.data_fim() == null || !date.isAfter(s.data_fim()));
            if (inRange) return s.id();
        }

        // Sem range de data: aplica semanal, mensal ou anual (primeira que bater)
        for (Sazonalidade s : sazonalidades) {
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
            Categoria.MenorIdade regra, int idade, double valorBase, int qtdCriancas) {

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
}
