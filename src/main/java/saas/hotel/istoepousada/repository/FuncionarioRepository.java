package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Funcionario.mapFuncionario;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Tela;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class FuncionarioRepository {
  Logger log = LoggerFactory.getLogger(FuncionarioRepository.class);
  private final JdbcTemplate jdbcTemplate;

  public FuncionarioRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final ResultSetExtractor<List<Funcionario>> FUNCIONARIO_EXTRACTOR =
      rs -> {
        Map<Long, Funcionario> funcionarioMap = new LinkedHashMap<>();
        Map<Long, Set<Tela>> telasPorFuncionario = new HashMap<>();

        while (rs.next()) {
          Long funcionarioId = rs.getLong("id");

          if (!funcionarioMap.containsKey(funcionarioId)) {
            Funcionario funcionario = mapFuncionario(rs);
            funcionarioMap.put(funcionarioId, funcionario);
            telasPorFuncionario.put(funcionarioId, new LinkedHashSet<>());
          }

          Long telaId = rs.getObject("tela_id", Long.class);
          if (telaId != null && telaId > 0) {
            Tela tela = Tela.mapTela(rs, "tela_");
            telasPorFuncionario.get(funcionarioId).add(tela);
          }
        }

        return funcionarioMap.values().stream()
            .map(
                func -> {
                  Set<Tela> telasSet = telasPorFuncionario.get(func.id());
                  List<Tela> telas = new ArrayList<>(telasSet);
                  Cargo cargoComTelas = func.cargo().withTelas(telas);
                  return new Funcionario(
                      func.id(), func.pessoa(), func.dataAdmissao(), cargoComTelas, func.usuario());
                })
            .toList();
      };

  public Page<Funcionario> buscar(Long id, String termo, Long cargoId, Pageable pageable) {
    boolean hasId = id != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();
    boolean hasCargoId = cargoId != null;

    String termoTrim = hasTermo ? termo.trim() : null;
    String search = hasTermo ? "%" + termoTrim + "%" : null;

    String baseSelect =
        """
                SELECT
                    f.id                           AS id,
                    f.data_admissao                AS data_admissao,

                    p.id                           AS pessoa_id,
                    p.data_hora_cadastro           AS pessoa_data_hora_cadastro,
                    p.nome                         AS pessoa_nome,
                    p.data_nascimento              AS pessoa_data_nascimento,
                    p.cpf                          AS pessoa_cpf,
                    p.rg                           AS pessoa_rg,
                    p.email                        AS pessoa_email,
                    p.telefone                     AS pessoa_telefone,
                    p.pais                         AS pessoa_pais,
                    p.estado                       AS pessoa_estado,
                    p.municipio                    AS pessoa_municipio,
                    p.endereco                     AS pessoa_endereco,
                    p.complemento                  AS pessoa_complemento,
                    p.vezes_hospedado              AS pessoa_vezes_hospedado,
                    p.cep                          AS pessoa_cep,
                    p.idade                        AS pessoa_idade,
                    p.bairro                       AS pessoa_bairro,
                    p.sexo                         AS pessoa_sexo,
                    p.numero                       AS pessoa_numero,
                    p.status                       AS pessoa_status,

                    c.id                           AS cargo_id,
                    c.cargo                        AS cargo_cargo,

                    u.id                           AS usuario_id,
                    u.username                     AS usuario_username,
                    u.bloqueado                    AS usuario_bloqueado,

                    t.id                           AS tela_id,
                    t.nome                         AS tela_nome,
                    t.descricao                    AS tela_descricao,
                    t.rota                         AS tela_rota,

                    p.fk_funcionario               AS pessoa_fk_funcionario,
                    p.fk_titular                   AS pessoa_fk_titular,

                    func.nome                      AS pessoa_funcionario_nome,
                    titular.nome                   AS pessoa_titular_nome

                FROM funcionario f
                INNER JOIN pessoa p ON p.id = f.fk_pessoa
                INNER JOIN cargo c ON c.id = f.fk_cargo
                LEFT JOIN usuario u ON u.id = f.fk_usuario
                LEFT JOIN cargo_tela ct ON ct.cargo_id = c.id
                LEFT JOIN tela t ON t.id = ct.tela_id
                LEFT JOIN pessoa func ON func.id = p.fk_funcionario
                LEFT JOIN pessoa titular ON titular.id = p.fk_titular
                """;

    StringBuilder where = new StringBuilder(" WHERE p.status = 'CONTRATADO'::pessoa_status ");
    List<Object> params = new ArrayList<>();

    if (hasId) {
      where.append(" AND f.id = ? ");
      params.add(id);
    }

    if (hasTermo) {
      where.append(" AND (p.nome ILIKE ? OR p.cpf = ?) ");
      params.add(search);
      params.add(termoTrim);
    }

    if (hasCargoId) {
      where.append(" AND c.id = ? ");
      params.add(cargoId);
    }

    String countSql =
        """
                SELECT COUNT(DISTINCT f.id)
                FROM funcionario f
                INNER JOIN pessoa p ON p.id = f.fk_pessoa
                INNER JOIN cargo c ON c.id = f.fk_cargo
                """
            + where;

    Long total;
    try {
      total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) return new PageImpl<>(List.of(), pageable, 0);

    String idsSql =
        """
                SELECT f.id
                FROM funcionario f
                INNER JOIN pessoa p ON p.id = f.fk_pessoa
                INNER JOIN cargo c ON c.id = f.fk_cargo
                """
            + where
            + """
        ORDER BY p.nome ASC
        LIMIT ? OFFSET ?
        """;

    List<Object> idsParams = new ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add((int) pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) return new PageImpl<>(List.of(), pageable, total);

    String inPlaceholders = String.join(",", Collections.nCopies(ids.size(), "?"));
    String pageSql =
        baseSelect + " WHERE f.id IN (" + inPlaceholders + ") " + " ORDER BY p.nome ASC";

    List<Funcionario> content = jdbcTemplate.query(pageSql, FUNCIONARIO_EXTRACTOR, ids.toArray());

    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Funcionario findById(Long id) {
    Page<Funcionario> page = buscar(id, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) throw new NotFoundException("Funcionário não encontrado para o id: " + id);
    return page.getContent().getFirst();
  }

  @Transactional
  public Funcionario insert(Long pessoaId, Long cargoId, LocalDate dataAdmissao, Long usuarioId) {
    String sql =
        """
                INSERT INTO funcionario (fk_pessoa, fk_cargo, data_admissao, fk_usuario)
                VALUES (?, ?, ?, ?)
                """;
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          ps.setLong(1, pessoaId);
          ps.setLong(2, cargoId);
          ps.setDate(3, Date.valueOf(dataAdmissao));
          if (usuarioId != null) ps.setLong(4, usuarioId);
          else ps.setNull(4, Types.BIGINT);
          return ps;
        },
        keyHolder);

    Long generatedId =
        keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")
            ? ((Number) keyHolder.getKeys().get("id")).longValue()
            : null;

    log.info("Funcionário criado: id={}, pessoaId={}, cargoId={}", generatedId, pessoaId, cargoId);
    return findById(generatedId);
  }

  public Funcionario findByUsuarioId(Long usuarioId) {
    String sql =
        """
            SELECT f.id
            FROM funcionario f
            WHERE f.fk_usuario = ?
            LIMIT 1
            """;

    try {
      Long funcionarioId = jdbcTemplate.queryForObject(sql, Long.class, usuarioId);
      return findById(funcionarioId);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Funcionário não encontrado para o usuário id: " + usuarioId);
    }
  }
}
