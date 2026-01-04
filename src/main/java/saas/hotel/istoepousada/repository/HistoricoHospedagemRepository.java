package saas.hotel.istoepousada.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.*;
import saas.hotel.istoepousada.enums.StatusQuarto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class HistoricoHospedagemRepository {

    private final JdbcTemplate jdbcTemplate;

    public HistoricoHospedagemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Regras:
     * - Busca por pessoaId + (dataInicio/dataFim) ou data única (passe dataInicio=dataFim)
     * - Retorna toda a hospedagem se houver interseção entre o range buscado e o período da hospedagem
     * - Se dataInicio e dataFim forem nulos => retorna o último histórico (mais recente) da pessoa
     *
     * Observação importante:
     * Este repositório assume que "hospedagem" é composta por pernoites vinculados à pessoa.
     * Se você tiver uma tabela "hospedagem" (id_hospedagem), adapte o filtro para usar esse id.
     */
    @Transactional(readOnly = true)
    public Optional<HistoricoHospedagem> buscarHistorico(Long pessoaId, LocalDate dataInicio, LocalDate dataFim) {
        if (pessoaId == null) return Optional.empty();

        Periodo periodo =
                (dataInicio == null && dataFim == null)
                        ? buscarUltimoPeriodo(pessoaId).orElse(null)
                        : new Periodo(
                        Objects.requireNonNullElse(dataInicio, dataFim),
                        Objects.requireNonNullElse(dataFim, dataInicio));

        if (periodo == null || periodo.inicio() == null || periodo.fim() == null) {
            return Optional.empty();
        }

        // Interseção (overlap): entrada <= fim AND saida >= inicio
        String sql =
                """
                SELECT
                  -- pernoite
                  pe.id                    AS pernoite_id,
                  pe.data_entrada          AS pernoite_data_entrada,
                  pe.data_saida            AS pernoite_data_saida,
                  pe.valot_total           AS pernoite_valor_total,
        
                  -- quarto + categoria
                  q.id                     AS quarto_id,
                  q.descricao              AS quarto_descricao,
                  q.quantidade_pessoas     AS quarto_quantidade_pessoas,
                  q.status_quarto_enum     AS quarto_status_quarto,
                  q.qtd_cama_casal         AS quarto_qtd_cama_casal,
                  q.qtd_cama_solteiro      AS quarto_qtd_cama_solteiro,
                  q.qtd_rede               AS quarto_qtd_rede,
                  q.qtd_beliche            AS quarto_qtd_beliche,
                  c.id                     AS cat_id,
                  c.categoria              AS cat_categoria,
        
                  -- diária (uma diária por pernoite por dia)
                  d.id                     AS diaria_id,
                  d.numero_diaria          AS diaria_numero,
                  d.data_inicio            AS diaria_data_inicio,
                  d.data_fim               AS diaria_data_fim,
                  d.valor_diaria           AS diaria_valor_diaria,
                  d.total                  AS diaria_total,
                  d.quantidade_pessoa      AS diaria_qtd_pessoas,
                  d.observacao             AS diaria_observacao,
        
                  -- pessoa(s) na diária
                  p.id                     AS pessoa_id,
                  p.nome                   AS pessoa_nome,
                  --                  AS representante, -- ajuste se não existir (titular/dependente)
        
                  -- pagamentos
                  pg.id                    AS pag_id,
                  pg.valor                 AS pag_valor,
                  pg.data_hora_pagamento   AS pag_data_hora,
                  tp.id                    AS tp_id,
                  tp.descricao             AS tp_descricao,
        
                  -- consumo
                  cs.id                    AS cons_id,
                  cs.data_hora_consumo             AS cons_data_hora,
                  cs.quantidade            AS cons_qtd,
                  --cs.valor_total           AS cons_valor_total, -- ajuste se seu consumo não tem valor_total
                  it.id                    AS item_id,
                  it.descricao             AS item_descricao,
                  ic.id                    AS item_cat_id,
                  ic.categoria             AS item_cat_categoria,
                  ctp.id                   AS cons_tp_id,
                  ctp.descricao            AS cons_tp_descricao
        
                FROM pernoite pe
                JOIN quarto q              ON q.id = pe.quarto_id
                LEFT JOIN categoria c      ON c.id = q.fk_categoria
        
                JOIN diaria d              ON d.pernoite_id = pe.id
        
                LEFT JOIN diaria_hospedes dp ON dp.diaria_id = d.id
                LEFT JOIN pessoa p         ON p.id = dp.hospedes_id
        
                LEFT JOIN diaria_pagamento pg ON pg.diaria_id = d.id
                LEFT JOIN tipo_pagamento tp   ON tp.id = pg.tipo_pagamento_id
        
                LEFT JOIN consumo_diaria cs   ON cs.diaria_id = d.id
                LEFT JOIN item it             ON it.id = cs.item_id
                LEFT JOIN categoria ic        ON ic.id = it.fk_categoria
                LEFT JOIN tipo_pagamento ctp  ON ctp.id = cs.tipo_pagamento_id
        
                WHERE dp.hospedes_id = ?
                  AND pe.data_entrada <= ?
                  AND pe.data_saida   >= ?
                ORDER BY d.numero_diaria, d.data_inicio, q.descricao, p.nome
                """;

        List<Object> params = List.of(pessoaId, periodo.fim(), periodo.inicio());

        HistoricoHospedagem historico =
                jdbcTemplate.query(sql, HISTORICO_EXTRACTOR(pessoaId, periodo.inicio(), periodo.fim()), params.toArray());

        return Optional.ofNullable(historico);
    }

    private Optional<Periodo> buscarUltimoPeriodo(Long pessoaId) {
        String sql =
                """
                SELECT pe.data_entrada, pe.data_saida
                  FROM pernoite pe
                  JOIN diaria d                ON d.pernoite_id = pe.id
                  JOIN diaria_hospedes dp      ON dp.diaria_id  = d.id
                 WHERE dp.hospedes_id = ?
                 ORDER BY pe.data_saida DESC
                 LIMIT 1
                """;
        try {
            return Optional.ofNullable(
                    jdbcTemplate.queryForObject(
                            sql,
                            (rs, rowNum) -> new Periodo(rs.getDate("data_entrada").toLocalDate(), rs.getDate("data_saida").toLocalDate()),
                            pessoaId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private record Periodo(LocalDate inicio, LocalDate fim) {}

    private ResultSetExtractor<HistoricoHospedagem> HISTORICO_EXTRACTOR(Long pessoaId, LocalDate inicio, LocalDate fim) {
        return rs -> {
            // Agrupa por número da diária (como a UI mostra 1ª, 2ª, 3ª…)
            Map<Integer, DiariaAgg> diarias = new LinkedHashMap<>();

            LocalDate minEntrada = null;
            LocalDate maxSaida = null;

            while (rs.next()) {
                LocalDate entrada = rs.getDate("pernoite_data_entrada").toLocalDate();
                LocalDate saida = rs.getDate("pernoite_data_saida").toLocalDate();
                if (minEntrada == null || entrada.isBefore(minEntrada)) minEntrada = entrada;
                if (maxSaida == null || saida.isAfter(maxSaida)) maxSaida = saida;

                Integer numero = getInt(rs, "diaria_numero");
                if (numero == null) continue;

                DiariaAgg agg = diarias.computeIfAbsent(numero, n -> {
                    try {
                        return new DiariaAgg(rs);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

                // Pessoas (evita duplicar por JOIN)
                Long pid = getLong(rs, "pessoa_id");
                if (pid != null) {
                    String nome = rs.getString("pessoa_nome");
//                    Boolean tipo = rs.getString("pessoa_tipo");
                    agg.pessoas.putIfAbsent(pid, new HistoricoHospedagem.PessoaResumo(pid, nome, false));
                }

                // Suítes (por pernoite)
                Long pernoiteId = getLong(rs, "pernoite_id");
                if (pernoiteId != null) {
                    agg.suites.putIfAbsent(pernoiteId, mapSuite(rs, pernoiteId));
                }

                // Pagamentos
                Long pagId = getLong(rs, "pag_id");
                if (pagId != null) {
                    agg.pagamentos.putIfAbsent(pagId, mapPagamento(rs, pagId));
                }

                // Consumos
                Long consId = getLong(rs, "cons_id");
                if (consId != null) {
                    agg.consumos.putIfAbsent(consId, mapConsumo(rs, consId));
                }
            }

            if (diarias.isEmpty()) return null;

            List<HistoricoHospedagem.DiariaHistorico> content =
                    diarias.values().stream().map(DiariaAgg::toRecord).toList();

            float total = 0f;
            for (var d : content) {
                if (d.subtotal() != null) total += d.subtotal();
            }

            return new HistoricoHospedagem(
                    pessoaId,
                    minEntrada != null ? minEntrada : inicio,
                    maxSaida != null ? maxSaida : fim,
                    total,
                    content);
        };
    }

    private static final class DiariaAgg {
        Integer numero;
        LocalDate dataInicio;
        LocalDate dataFim;
        Integer qtdPessoas;
        String observacao;
        Float subtotal;

        Map<Long, HistoricoHospedagem.PessoaResumo> pessoas = new LinkedHashMap<>();
        Map<Long, HistoricoHospedagem.SuiteResumo> suites = new LinkedHashMap<>();
        Map<Long, HistoricoHospedagem.PagamentoResumo> pagamentos = new LinkedHashMap<>();
        Map<Long, HistoricoHospedagem.ConsumoResumo> consumos = new LinkedHashMap<>();

        DiariaAgg(ResultSet rs) throws SQLException {
            this.numero = getInt(rs, "diaria_numero");
            this.dataInicio = rs.getDate("diaria_data_inicio") != null ? rs.getDate("diaria_data_inicio").toLocalDate() : null;
            this.dataFim = rs.getDate("diaria_data_fim") != null ? rs.getDate("diaria_data_fim").toLocalDate() : null;
            this.qtdPessoas = getInt(rs, "diaria_qtd_pessoas");
            this.observacao = rs.getString("diaria_observacao");
            this.subtotal = getFloat(rs, "diaria_total");
            if (this.subtotal == null) this.subtotal = getFloat(rs, "diaria_valor_diaria");
        }

        HistoricoHospedagem.DiariaHistorico toRecord() {
            return new HistoricoHospedagem.DiariaHistorico(
                    numero,
                    dataInicio,
                    dataFim,
                    qtdPessoas,
                    observacao,
                    subtotal,
                    new ArrayList<>(pessoas.values()),
                    new ArrayList<>(suites.values()),
                    new ArrayList<>(pagamentos.values()),
                    new ArrayList<>(consumos.values()));
        }
    }

    // -------- mapeamentos auxiliares (ajuste nomes/colunas conforme seu schema) --------

    private static HistoricoHospedagem.SuiteResumo mapSuite(ResultSet rs, Long pernoiteId) throws SQLException {
        Quarto quarto = mapQuarto(rs);
        Float valor = getFloat(rs, "pernoite_valor_total"); // ou valor da diária/quarto, conforme seu modelo

        // Responsável (opcional) — se você tiver um campo pra isso, ajuste; aqui uso a "primeira pessoa" da linha
        Long pessoaId = getLong(rs, "pessoa_id");
        HistoricoHospedagem.PessoaResumo resp =
                pessoaId == null
                        ? null
                        : new HistoricoHospedagem.PessoaResumo(pessoaId, rs.getString("pessoa_nome"), false);

        return new HistoricoHospedagem.SuiteResumo(pernoiteId, quarto, valor, resp);
    }

    private static HistoricoHospedagem.PagamentoResumo mapPagamento(ResultSet rs, Long pagId) throws SQLException {
        Float valor = getFloat(rs, "pag_valor");
        LocalDateTime dh = rs.getTimestamp("pag_data_hora") != null ? rs.getTimestamp("pag_data_hora").toLocalDateTime() : null;
        TipoPagamento tp = new TipoPagamento(getLong(rs, "tp_id"), rs.getString("tp_descricao"));
        return new HistoricoHospedagem.PagamentoResumo(pagId, valor, dh, tp);
    }

    private static HistoricoHospedagem.ConsumoResumo mapConsumo(ResultSet rs, Long consId) throws SQLException {
        LocalDateTime dh =
                rs.getTimestamp("cons_data_hora") != null ? rs.getTimestamp("cons_data_hora").toLocalDateTime() : null;
        Integer qtd = getInt(rs, "cons_qtd");
        Float valorTotal = getFloat(rs, "cons_valor_total");

        Categoria cat = new Categoria(getLong(rs, "item_cat_id"), rs.getString("item_cat_categoria"));
        Item item = new Item(getLong(rs, "item_id"), rs.getString("item_descricao"), cat, null);

        TipoPagamento tp = new TipoPagamento(getLong(rs, "cons_tp_id"), rs.getString("cons_tp_descricao"));

        return new HistoricoHospedagem.ConsumoResumo(consId, dh, item, qtd, valorTotal, tp);
    }

    private static Quarto mapQuarto(ResultSet rs) throws SQLException {
        return new Quarto(
                getLong(rs, "quarto_id"),
                rs.getString("quarto_descricao"),
                getInt(rs, "quarto_quantidade_pessoas"),
StatusQuarto.DISPONIVEL,
//                rs.getString("quarto_status_quarto") != null
//                        ? StatusQuarto.valueOf(rs.getString("quarto_status_quarto"))
//                        : null,
                getInt(rs, "quarto_qtd_cama_casal"),
                getInt(rs, "quarto_qtd_cama_solteiro"),
                getInt(rs, "quarto_qtd_rede"),
                getInt(rs, "quarto_qtd_beliche"));
    }

    private static Long getLong(ResultSet rs, String col) throws SQLException {
        Object v = rs.getObject(col);
        return v == null ? null : ((Number) v).longValue();
    }

    private static Integer getInt(ResultSet rs, String col) throws SQLException {
        Object v = rs.getObject(col);
        return v == null ? null : ((Number) v).intValue();
    }

    private static Float getFloat(ResultSet rs, String col) throws SQLException {
        Object v = rs.getObject(col);
        return v == null ? null : ((Number) v).floatValue();
    }
}
