package saas.hotel.istoepousada.repository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.*;

/**
 * Histórico de hospedagem (por pessoa) com suporte a: - sem datas: último histórico (pernoite mais
 * recente) - só dataInicio: desta data em diante (pe.data_saida >= dataInicio) - só dataFim: desta
 * data pra trás (pe.data_entrada <= dataFim) - dataInicio + dataFim: range por "overlap"
 * (pe.data_entrada <= dataFim AND pe.data_saida >= dataInicio)
 *
 * <p>Cálculos: - tipoHospedagem: baseado em diaria.quantidadePessoas; se variar entre diárias,
 * concatena (ex: "DUPLA, TRIPLA") - subTotal diária: (diaria.total se existir, senão
 * diaria.valorDiaria) + soma(pagamentos.valor) + soma(consumosValor) Observação: sua tabela
 * diaria_consumo não tem valor; consumo fica 0 no subtotal (mas lista os consumos). -
 * valorTotalHospedagem: soma dos subtotais de todas as diárias do pernoite - totalDiasHospedado:
 * soma do número de diárias de todos os pernoites (cada linha de diaria = 1 dia) - valorTotal: soma
 * dos valores de todas as hospedagens (pernoites)
 *
 * <p>Mappers usados (dos records): - Pernoite.mapPernoite(rs, "pernoite_") - Diaria.mapDiaria(rs,
 * "diaria_") (que chama Quarto.mapQuarto(rs) internamente) - Pessoa.mapPessoa(rs, "pessoa_") -
 * Item.mapItem(rs, "item_") (que chama Categoria.mapCategoria(rs)) -
 * DiariaPagamento.mapDiariaPagamento(rs, "diaria_pagamento_")
 *
 * <p>TipoPagamento: montado manualmente em pagamento/consumo por causa de aliases distintos.
 */
@Repository
public class HistoricoHospedagemRepository {

  private final JdbcTemplate jdbcTemplate;

  public HistoricoHospedagemRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional(readOnly = true)
  public Optional<HistoricoHospedagem> buscarHistorico(
      Long pessoaId, LocalDate dataInicio, LocalDate dataFim) {
    if (pessoaId == null) return Optional.empty();

    // Sem datas -> último pernoite (mais recente)
    if (dataInicio == null && dataFim == null) {
      Long ultimoPernoiteId = buscarUltimoPernoiteId(pessoaId).orElse(null);
      if (ultimoPernoiteId == null) return Optional.empty();

      String sql =
          buildBaseSql()
              + """
              WHERE dpf.pessoa_id = ?
                AND pe.id = ?
              ORDER BY pe.data_entrada DESC, d.numero_diaria ASC, d.data_inicio ASC, dp.representante DESC, p.nome ASC
              """;

      HistoricoHospedagem historico =
          jdbcTemplate.query(sql, HISTORICO_EXTRACTOR(), pessoaId, pessoaId, ultimoPernoiteId);

      return Optional.ofNullable(historico);
    }

    // Com datas -> regras novas
    StringBuilder where = new StringBuilder(" WHERE dpf.pessoa_id = ? ");
    List<Object> params = new ArrayList<>();
    params.add(pessoaId);
    params.add(pessoaId);

    // dataInicio + dataFim => overlap fechado
    if (dataInicio != null && dataFim != null) {
      where.append(" AND pe.data_entrada <= ? AND pe.data_saida >= ? ");
      params.add(dataFim);
      params.add(dataInicio);
    }
    // somente inicio => datas em diante
    else if (dataInicio != null) {
      where.append(" AND pe.data_saida >= ? ");
      params.add(dataInicio);
    }
    // somente fim => datas pra trás
    else {
      where.append(" AND pe.data_entrada <= ? ");
      params.add(dataFim);
    }

    String sql =
        buildBaseSql()
            + where
            + """
              ORDER BY pe.data_entrada DESC, d.numero_diaria ASC, d.data_inicio ASC, dp.representante DESC, p.nome ASC
              """;

    HistoricoHospedagem historico =
        jdbcTemplate.query(sql, HISTORICO_EXTRACTOR(), params.toArray());

    return Optional.ofNullable(historico);
  }

  private String buildBaseSql() {
    return """
        SELECT
          -- pernoite (prefix pernoite_)
          pe.id                 AS pernoite_id,
          pe.data_entrada       AS pernoite_data_entrada,
          pe.data_saida         AS pernoite_data_saida,
          pe.status             AS pernoite_status,
          pe.hora_chegada       AS pernoite_hora_chegada,
          pe.hora_saida         AS pernoite_hora_saida,
          pe.valor_total        AS pernoite_valor_total,
          pe.ativo              AS pernoite_ativo,

          -- diária (prefix diaria_)
          d.id                  AS diaria_id,
          d.data_inicio         AS diaria_data_inicio,
          d.data_fim            AS diaria_data_fim,
          d.valor_diaria        AS diaria_valor,
          d.total               AS diaria_total,
          d.numero_diaria       AS diaria_numero,
          d.quantidade_pessoa   AS diaria_quantidade_pessoa,
          d.observacao          AS diaria_observacao,

          -- quarto (prefix quarto_) (Diaria.mapDiaria -> Quarto.mapQuarto)
          q.id                  AS quarto_id,
          q.descricao           AS quarto_descricao,
          q.quantidade_pessoas  AS quarto_qtd_pessoas,
          q.status_quarto_enum  AS quarto_status,
          q.qtd_cama_casal      AS quarto_qtd_cama_casal,
          q.qtd_cama_solteiro   AS quarto_qtd_cama_solteiro,
          q.qtd_rede            AS quarto_qtd_rede,
          q.qtd_beliche         AS quarto_qtd_beliche,

          -- marca se a pessoa da linha é representante
          dp.representante      AS diaria_pessoa_representante,

          -- pessoa (prefix pessoa_)
          p.id                  AS pessoa_id,
          p.data_hora_cadastro  AS pessoa_data_hora_cadastro,
          p.nome                AS pessoa_nome,
          p.data_nascimento     AS pessoa_data_nascimento,
          p.cpf                 AS pessoa_cpf,
          p.rg                  AS pessoa_rg,
          p.email               AS pessoa_email,
          p.telefone            AS pessoa_telefone,
          p.fk_pais             AS pessoa_fk_pais,
          p.fk_estado           AS pessoa_fk_estado,
          p.fk_municipio        AS pessoa_fk_municipio,
          p.endereco            AS pessoa_endereco,
          p.complemento         AS pessoa_complemento,
          p.hospedado           AS pessoa_hospedado,
          p.vezes_hospedado     AS pessoa_vezes_hospedado,
          p.cliente_novo        AS pessoa_cliente_novo,
          p.cep                 AS pessoa_cep,
          p.idade               AS pessoa_idade,
          p.bairro              AS pessoa_bairro,
          p.sexo                AS pessoa_sexo,
          p.numero              AS pessoa_numero,
          p.bloqueado           AS pessoa_bloqueado,

          -- pagamentos (prefix diaria_pagamento_)
          pg.id                 AS diaria_pagamento_id,
          pg.descricao          AS diaria_pagamento_descricao,
          pg.valor              AS diaria_pagamento_valor,
          pg.data_hora_pagamento AS diaria_pagamento_data_hora,
          tp.id                  AS diaria_pagamento_tipo_pagamento_id,
          tp.descricao           AS diaria_pagamento_tipo_pagamento_descricao,

          -- consumos (prefix diaria_consumo_) + item (prefix item_) + categoria_item (prefix categoria_)
          cs.id                 AS diaria_consumo_id,
          cs.data_hora_consumo  AS diaria_consumo_data_hora,
          cs.quantidade         AS diaria_consumo_quantidade,
          ctp.id                AS consumo_tipo_pagamento_id,
          ctp.descricao         AS consumo_tipo_pagamento_descricao,

          it.id                 AS item_id,
          it.descricao          AS item_descricao,
          it.data_hora_registro_item AS item_data_hora,
          ci.id                 AS categoria_id,
          ci.descricao          AS categoria_categoria

        FROM pernoite pe
        JOIN diaria d ON d.pernoite_id = pe.id
        LEFT JOIN quarto q ON q.id = d.quarto_id

        -- filtro principal: garante que a pessoa informada pertence à diária
        JOIN diaria_pessoa dpf ON dpf.diaria_id = d.id AND dpf.pessoa_id = ?

        -- lista todos os hóspedes da diária (rep + acompanhantes)
        LEFT JOIN diaria_pessoa dp ON dp.diaria_id = d.id
        LEFT JOIN pessoa p ON p.id = dp.pessoa_id

        -- pagamentos
        LEFT JOIN diaria_pagamento pg ON pg.diaria_id = d.id
        LEFT JOIN tipo_pagamento tp ON tp.id = pg.tipo_pagamento_id

        -- consumos
        LEFT JOIN diaria_consumo cs ON cs.diaria_id = d.id
        LEFT JOIN item it ON it.id = cs.item_id
        LEFT JOIN categoria_item ci ON ci.id = it.fk_categoria
        LEFT JOIN tipo_pagamento ctp ON ctp.id = cs.tipo_pagamento_id
        """;
  }

  private ResultSetExtractor<HistoricoHospedagem> HISTORICO_EXTRACTOR() {
    return rs -> {
      Map<Long, PernoiteAgg> pernoites = new LinkedHashMap<>();
      Set<Integer> qtdPessoasSet = new TreeSet<>();

      while (rs.next()) {
        Long pernoiteId = rs.getObject("pernoite_id", Long.class);
        if (pernoiteId == null) continue;

        PernoiteAgg pAgg =
            pernoites.computeIfAbsent(
                pernoiteId,
                id -> {
                  try {
                    return new PernoiteAgg(Pernoite.mapPernoite(rs, "pernoite_"));
                  } catch (SQLException e) {
                    throw new RuntimeException(e);
                  }
                });

        Long diariaId = rs.getObject("diaria_id", Long.class);
        if (diariaId == null) continue;

        DiariaAgg dAgg =
            pAgg.diarias.computeIfAbsent(
                diariaId,
                id -> {
                  try {
                    return new DiariaAgg(Diaria.mapDiaria(rs, "diaria_"));
                  } catch (SQLException e) {
                    throw new RuntimeException(e);
                  }
                });

        Integer qtdPessoas = rs.getObject("diaria_quantidade_pessoa", Integer.class);
        if (qtdPessoas != null) qtdPessoasSet.add(qtdPessoas);

        // pessoa (rep/acompanhante)
        Long hospedeId = rs.getObject("pessoa_id", Long.class);
        if (hospedeId != null) {
          boolean representante =
              Boolean.TRUE.equals(rs.getObject("diaria_pessoa_representante", Boolean.class));
          Pessoa pessoa = Pessoa.mapPessoa(rs, "pessoa_");

          if (representante) {
            dAgg.representante = pessoa;
          } else {
            dAgg.acompanhantes.putIfAbsent(hospedeId, pessoa);
          }
        }

        // pagamento
        Long pagId = rs.getObject("diaria_pagamento_id", Long.class);
        if (pagId != null) {
          DiariaPagamento pagamento = DiariaPagamento.mapDiariaPagamento(rs, "diaria_pagamento_");
          if (pagamento.tipo_pagamento() == null) {
            Long tpId = rs.getObject("tipo_pagamento_id", Long.class);
            String tpDesc = rs.getString("tipo_pagamento_descricao");
            TipoPagamento tp =
                (tpId == null && tpDesc == null) ? null : new TipoPagamento(tpId, tpDesc);
            pagamento =
                new DiariaPagamento(
                    pagamento.id(),
                    pagamento.descricao(),
                    pagamento.valor(),
                    pagamento.data_hora(),
                    tp);
          }

          dAgg.pagamentos.putIfAbsent(pagId, pagamento);
        }

        // consumo
        Long consId = rs.getObject("diaria_consumo_id", Long.class);
        if (consId != null) {
          LocalDateTime dh =
              rs.getTimestamp("diaria_consumo_data_hora") != null
                  ? rs.getTimestamp("diaria_consumo_data_hora").toLocalDateTime()
                  : null;

          Integer quantidade = rs.getObject("diaria_consumo_quantidade", Integer.class);
          Item item = Item.mapItem(rs, "item_");

          Long tpId = rs.getObject("consumo_tipo_pagamento_id", Long.class);
          String tpDesc = rs.getString("consumo_tipo_pagamento_descricao");
          TipoPagamento tp =
              (tpId == null && tpDesc == null) ? null : new TipoPagamento(tpId, tpDesc);

          DiariaConsumo consumo = new DiariaConsumo(consId, dh, item, quantidade, tp);
          dAgg.consumos.putIfAbsent(consId, consumo);
        }
      }

      if (pernoites.isEmpty()) return null;

      List<HistoricoHospedagem.DadosPernoite> pernoitesOut = new ArrayList<>(pernoites.size());

      float valorTotalGeral = 0f;
      int totalDiasHospedado = 0;

      for (PernoiteAgg pAgg : pernoites.values()) {
        List<HistoricoHospedagem.DadosPernoite.DadosDiaria> diariasOut =
            new ArrayList<>(pAgg.diarias.size());

        float valorTotalHospedagem = 0f;

        for (DiariaAgg dAgg : pAgg.diarias.values()) {
          totalDiasHospedado++;

          float base =
              dAgg.diaria.total() != null
                  ? safe(dAgg.diaria.total())
                  : safe(dAgg.diaria.valorDiaria());

          float somaPag =
              (float)
                  dAgg.pagamentos.values().stream()
                      .filter(Objects::nonNull)
                      .map(DiariaPagamento::valor)
                      .filter(Objects::nonNull)
                      .mapToDouble(Float::doubleValue)
                      .sum();

          float somaCons = 0f;

          float subTotal = base + somaPag + somaCons;
          valorTotalHospedagem += subTotal;

          diariasOut.add(
              new HistoricoHospedagem.DadosPernoite.DadosDiaria(
                  dAgg.diaria,
                  dAgg.representante,
                  new ArrayList<>(dAgg.acompanhantes.values()),
                  new ArrayList<>(dAgg.pagamentos.values()),
                  new ArrayList<>(dAgg.consumos.values()),
                  subTotal));
        }

        valorTotalGeral += valorTotalHospedagem;

        pernoitesOut.add(
            new HistoricoHospedagem.DadosPernoite(pAgg.pernoite, diariasOut, valorTotalHospedagem));
      }

      String tipoHospedagem =
          qtdPessoasSet.isEmpty()
              ? null
              : qtdPessoasSet.stream()
                  .map(this::mapTipoHospedagem)
                  .distinct()
                  .reduce((a, b) -> a + ", " + b)
                  .orElse(null);

      return new HistoricoHospedagem(
          tipoHospedagem, pernoitesOut.size(), totalDiasHospedado, valorTotalGeral, pernoitesOut);
    };
  }

  private Optional<Long> buscarUltimoPernoiteId(Long pessoaId) {
    String sql =
        """
                SELECT pe.id
                  FROM pernoite pe
                  JOIN diaria d ON d.pernoite_id = pe.id
                  JOIN diaria_pessoa dp ON dp.diaria_id = d.id
                 WHERE dp.pessoa_id = ?
                 ORDER BY pe.data_saida DESC NULLS LAST, pe.data_entrada DESC NULLS LAST, pe.id DESC
                 LIMIT 1
                """;
    try {
      return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Long.class, pessoaId));
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  private String mapTipoHospedagem(Integer qtdPessoas) {
    if (qtdPessoas == null || qtdPessoas <= 0) return "HOSPEDAGEM";
    return switch (qtdPessoas) {
      case 1 -> "HOSPEDAGEM INDIVIDUAL";
      case 2 -> "HOSPEDAGEM DUPLA";
      case 3 -> "HOSPEDAGEM TRIPLA";
      case 4 -> "HOSPEDAGEM QUÁDRUPLA";
      default -> "HOSPEDAGEM " + qtdPessoas + " PESSOAS";
    };
  }

  private static float safe(Float v) {
    return v == null ? 0f : v;
  }

  private static final class PernoiteAgg {
    final Pernoite pernoite;
    final Map<Long, DiariaAgg> diarias = new LinkedHashMap<>();

    PernoiteAgg(Pernoite pernoite) {
      this.pernoite = pernoite;
    }
  }

  private static final class DiariaAgg {
    final Diaria diaria;

    Pessoa representante;
    final Map<Long, Pessoa> acompanhantes = new LinkedHashMap<>();
    final Map<Long, DiariaPagamento> pagamentos = new LinkedHashMap<>();
    final Map<Long, DiariaConsumo> consumos = new LinkedHashMap<>();

    DiariaAgg(Diaria diaria) {
      this.diaria = diaria;
    }
  }
}
