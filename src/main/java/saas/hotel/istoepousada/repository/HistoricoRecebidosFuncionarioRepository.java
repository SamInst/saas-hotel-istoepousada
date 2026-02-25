package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.HistoricoRecebidosFuncionario;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class HistoricoRecebidosFuncionarioRepository {

    private final JdbcTemplate jdbcTemplate;

    public HistoricoRecebidosFuncionarioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HistoricoRecebidosFuncionario> buscar(Long historicoFuncionarioId) {
        String sql =
                """
                SELECT
                  hrf.id AS id,
                  hrf.fk_historico_funcionario AS historico_funcionario_id,
                  hrf.valor_recebido AS valor_recebido,
                  hrf.data_hora_inicio AS data_hora_inicio,
                  hrf.data_hora_fim AS data_hora_fim,
                  hrf.data_hora_pagamento AS data_hora_pagamento,
                  hrf.fk_tipo_pagamento AS tipo_pagamento_id,
                  tp.descricao AS tipo_pagamento_descricao,
                  hrf.descricao AS descricao,
                  hrf.path_arquivo_comprovante AS path_arquivo_comprovante
                FROM historico_recebidos_funcionario hrf
                JOIN tipo_pagamento tp ON tp.id = hrf.fk_tipo_pagamento
                WHERE hrf.fk_historico_funcionario = ?
                ORDER BY hrf.data_hora_inicio DESC, hrf.id DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> HistoricoRecebidosFuncionario.mapHistoricoRecebidosFuncionario(rs),
                historicoFuncionarioId);
    }

    public HistoricoRecebidosFuncionario findById(Long id) {
        String sql =
                """
                SELECT
                  hrf.id AS id,
                  hrf.fk_historico_funcionario AS historico_funcionario_id,
                  hrf.valor_recebido AS valor_recebido,
                  hrf.data_hora_inicio AS data_hora_inicio,
                  hrf.data_hora_fim AS data_hora_fim,
                  hrf.data_hora_pagamento AS data_hora_pagamento,
                  hrf.fk_tipo_pagamento AS tipo_pagamento_id,
                  tp.descricao AS tipo_pagamento_descricao,
                  hrf.descricao AS descricao,
                  hrf.path_arquivo_comprovante AS path_arquivo_comprovante
                FROM historico_recebidos_funcionario hrf
                JOIN tipo_pagamento tp ON tp.id = hrf.fk_tipo_pagamento
                WHERE hrf.id = ?
                """;

        try {
            return jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> HistoricoRecebidosFuncionario.mapHistoricoRecebidosFuncionario(rs),
                    id);
        } catch (EmptyResultDataAccessException ex) {
            throw new NotFoundException("Recebido não encontrado para o id: " + id);
        }
    }

    @Transactional
    public HistoricoRecebidosFuncionario insert(HistoricoRecebidosFuncionario r) {
        String sql =
                """
                INSERT INTO historico_recebidos_funcionario
                  (fk_historico_funcionario, valor_recebido, data_hora_inicio, data_hora_fim, data_hora_pagamento,
                   fk_tipo_pagamento, descricao, path_arquivo_comprovante)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, r.historicoFuncionario().id());
                    ps.setFloat(2, r.valorRecebido());
                    ps.setObject(3, r.dataHoraInicio());
                    ps.setObject(4, r.dataHoraFim());
                    ps.setObject(5, r.dataHoraPagamento());
                    ps.setLong(6, r.tipoPagamento().id());
                    ps.setString(7, r.descricao());
                    ps.setString(8, r.pathArquivoComprovante());
                    return ps;
                },
                keyHolder);

        Long generatedId =
                keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")
                        ? ((Number) keyHolder.getKeys().get("id")).longValue()
                        : null;

        return r.withId(generatedId);
    }

    @Transactional
    public HistoricoRecebidosFuncionario update(HistoricoRecebidosFuncionario r) {
        findById(r.id());

        String sql =
                """
                UPDATE historico_recebidos_funcionario
                SET
                  fk_historico_funcionario = ?,
                  valor_recebido = ?,
                  data_hora_inicio = ?,
                  data_hora_fim = ?,
                  data_hora_pagamento = ?,
                  fk_tipo_pagamento = ?,
                  descricao = ?,
                  path_arquivo_comprovante = ?
                WHERE id = ?
                """;

        jdbcTemplate.update(
                sql,
                r.historicoFuncionario().id(),
                r.valorRecebido(),
                r.dataHoraInicio(),
                r.dataHoraFim(),
                r.dataHoraPagamento(),
                r.tipoPagamento().id(),
                r.descricao(),
                r.pathArquivoComprovante(),
                r.id());

        return findById(r.id());
    }
}
