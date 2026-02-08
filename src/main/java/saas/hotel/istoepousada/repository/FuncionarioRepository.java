package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Funcionario.mapFuncionario;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
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
        Map<Long, Funcionario> funcMap = new LinkedHashMap<>();
        Map<Long, LinkedHashMap<Long, Tela>> telasPorFunc = new HashMap<>();

        Map<Long, Map<Long, List<saas.hotel.istoepousada.dto.Permissao>>> permsPorFuncTela =
            new HashMap<>();
        Map<Long, Map<Long, Set<Long>>> permIdsPorFuncTela = new HashMap<>();

        while (rs.next()) {
          Long funcId = rs.getLong("id");

          if (!funcMap.containsKey(funcId)) {
            Funcionario f = mapFuncionario(rs);
            funcMap.put(funcId, f);
            telasPorFunc.put(funcId, new LinkedHashMap<>());
            permsPorFuncTela.put(funcId, new HashMap<>());
            permIdsPorFuncTela.put(funcId, new HashMap<>());
          }

          Long telaId = rs.getObject("tela_id", Long.class);
          if (telaId != null && telaId > 0) {
            LinkedHashMap<Long, Tela> telasMap = telasPorFunc.get(funcId);

            Tela tela = telasMap.get(telaId);
            if (tela == null) {
              tela = Tela.mapTela(rs, "tela_");
              telasMap.put(telaId, tela);
            }

            saas.hotel.istoepousada.dto.Permissao perm =
                saas.hotel.istoepousada.dto.Permissao.mapPermissao(rs, "permissao_");

            if (perm != null && perm.id() != null) {
              permsPorFuncTela.get(funcId).computeIfAbsent(telaId, k -> new ArrayList<>());
              permIdsPorFuncTela.get(funcId).computeIfAbsent(telaId, k -> new HashSet<>());

              if (permIdsPorFuncTela.get(funcId).get(telaId).add(perm.id())) {
                permsPorFuncTela.get(funcId).get(telaId).add(perm);
              }
            }
          }
        }

        List<Funcionario> out = new ArrayList<>();
        for (var entry : funcMap.entrySet()) {
          Long funcId = entry.getKey();
          Funcionario f = entry.getValue();

          List<Tela> telas =
              telasPorFunc.get(funcId).values().stream()
                  .map(
                      t ->
                          t.withPermissoes(
                              permsPorFuncTela
                                  .getOrDefault(funcId, Map.of())
                                  .getOrDefault(t.id(), List.of())))
                  .toList();

          Cargo cargo = (f.cargo() == null) ? null : f.cargo().withTelas(telas);

          out.add(new Funcionario(f.id(), f.pessoa(), f.dataAdmissao(), cargo, f.usuario()));
        }

        return out;
      };

  public Page<Funcionario> buscar(
      Long id, String termo, Long cargoId, Long pessoaId, Long usuarioId, Pageable pageable) {
    boolean hasId = id != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();
    boolean hasCargoId = cargoId != null;
    boolean hasPessoaId = pessoaId != null;
    boolean hasUsuarioId = usuarioId != null;

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

                        perm.id                        AS permissao_id,
                        perm.permissao                 AS permissao_permissao,
                        perm.descricao                 AS permissao_descricao,
                        perm.fk_tela                   AS permissao_fk_tela,

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
                    LEFT JOIN cargo_permissao cp ON cp.fk_cargo = c.id
                    LEFT JOIN permissao perm
                           ON perm.id = cp.fk_permissao
                          AND perm.fk_tela = t.id
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

    if (hasPessoaId) {
      where.append(" AND p.id = ? ");
      params.add(pessoaId);
    }

    if (hasUsuarioId) {
      where.append(" AND u.id = ? ");
      params.add(usuarioId);
    }

    String countSql =
        """
                    SELECT COUNT(DISTINCT f.id)
                    FROM funcionario f
                    INNER JOIN pessoa p ON p.id = f.fk_pessoa
                    INNER JOIN cargo c ON c.id = f.fk_cargo
                    LEFT JOIN usuario u ON u.id = f.fk_usuario
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
                    LEFT JOIN usuario u ON u.id = f.fk_usuario
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
    Page<Funcionario> page = buscar(id, null, null, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) throw new NotFoundException("Funcionário não encontrado para o id: " + id);
    return page.getContent().getFirst();
  }

  @Transactional
  public Funcionario insert(Long pessoaId, Funcionario.FuncionarioRequest request, Long usuarioId) {
    String sql =
        """
                    INSERT INTO funcionario (fk_pessoa, fk_cargo, data_admissao, fk_usuario, salario)
                    VALUES (?, ?, ?, ?, ?)
                    """;
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          ps.setLong(1, pessoaId);
          ps.setLong(2, request.cargoId());
          ps.setDate(3, Date.valueOf(request.dataAdmissao()));
          if (usuarioId != null) ps.setLong(4, usuarioId);
          else ps.setNull(4, Types.BIGINT);

          if (request.salario() != null) {
            ps.setFloat(5, request.salario());
          } else {
            ps.setNull(5, Types.NUMERIC);
          }
          return ps;
        },
        keyHolder);

    Long generatedId =
        keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")
            ? ((Number) keyHolder.getKeys().get("id")).longValue()
            : null;

    log.info(
        "Funcionário criado: id={}, pessoaId={}, cargoId={}",
        generatedId,
        pessoaId,
        request.cargoId());
    return findById(generatedId);
  }

  public Funcionario findByUsuarioId(Long usuarioId) {
    Page<Funcionario> page = buscar(null, null, null, null, usuarioId, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Funcionário não encontrado para o usuario id: " + usuarioId);
    }
    return page.getContent().getFirst();
  }

  public Funcionario findByPessoaId(Long pessoaId) {
    Page<Funcionario> page = buscar(null, null, null, pessoaId, null, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Funcionário não encontrado para a pessoa id: " + pessoaId);
    }
    return page.getContent().getFirst();
  }
}
