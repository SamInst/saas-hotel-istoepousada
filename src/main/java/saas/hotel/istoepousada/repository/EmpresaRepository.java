package saas.hotel.istoepousada.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class EmpresaRepository {
    private final JdbcTemplate jdbc_template;
    private final PessoaRepository pessoa_repository;

    public EmpresaRepository(JdbcTemplate jdbcTemplate, PessoaRepository pessoaRepository) {
        this.jdbc_template = jdbcTemplate;
        pessoa_repository = pessoaRepository;
    }

    public Page<Empresa> findByIdNomeOuCnpj(Long id, String termo, Pageable pageable) {
        boolean has_id = id != null;
        boolean has_termo = termo != null && !termo.trim().isEmpty();
        String termo_trim = has_termo ? termo.trim() : null;
        String search = has_termo ? "%" + termo_trim + "%" : null;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (has_id) {
            where.append(" AND e.id = ? ");
            params.add(id);
        }

        if (has_termo) {
            where.append(" AND (e.razao_social ILIKE ? OR e.nome_fantasia ILIKE ? OR e.cnpj = ?) ");
            params.add(search);
            params.add(search);
            params.add(termo_trim);
        }

        long total =
                jdbc_template.queryForObject(
                        "SELECT COUNT(*) FROM empresa e" + where,
                        Long.class,
                        params.toArray());

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Object> query_params = new ArrayList<>(params);
        query_params.add(pageable.getPageSize());
        query_params.add(pageable.getOffset());

        String sql =
                """
                        SELECT
                            e.id,
                            e.data_hora_registro,
                            e.razao_social,
                            e.nome_fantasia,
                            e.cnpj,
                            e.telefone,
                            e.email,
                            e.endereco,
                            e.cep,
                            e.numero,
                            e.complemento,
                            e.pais,
                            e.estado,
                            e.municipio,
                            e.bairro,
                            e.tipo_empresa,
                            e.status
                        FROM empresa e
                        """
                        + where
                        + """
                        ORDER BY e.razao_social
                        LIMIT ? OFFSET ?
                        """;

        List<Empresa> empresas =
                jdbc_template.query(sql, Empresa.ROW_MAPPER, query_params.toArray());

        List<Empresa> content =
                empresas.stream()
                        .map(
                                empresa -> {
                                    List<Pessoa> pessoas_vinculadas = findPessoasByEmpresaId(empresa.id());
                                    return new Empresa(
                                            empresa.id(),
                                            empresa.data_hora_registro(),
                                            empresa.razao_social(),
                                            empresa.nome_fantasia(),
                                            empresa.cnpj(),
                                            empresa.telefone(),
                                            empresa.email(),
                                            empresa.endereco(),
                                            empresa.cep(),
                                            empresa.numero(),
                                            empresa.complemento(),
                                            empresa.pais(),
                                            empresa.estado(),
                                            empresa.municipio(),
                                            empresa.bairro(),
                                            empresa.tipo_empresa(),
                                            empresa.status(),
                                            new Funcionario.Nome(
                                                    empresa.funcionario().id(),
                                                    empresa.funcionario().nome()
                                            ),
                                            pessoas_vinculadas);
                                })
                        .toList();

        return new PageImpl<>(content, pageable, total);
    }

    public Optional<Empresa> findById(Long id) {
        Page<Empresa> page = findByIdNomeOuCnpj(id, null, Pageable.ofSize(1));
        if (page.isEmpty()) throw new NotFoundException("Empresa não cadastrada para o id: " + id);

        return Optional.of(page.getContent().getFirst());
    }


    public Empresa create(Empresa.Update empresa) {
        String sql =
                """
                        INSERT INTO empresa (
                            razao_social,
                            nome_fantasia,
                            cnpj,
                            telefone,
                            email,
                            endereco,
                            cep,
                            numero,
                            complemento,
                            status,
                            fk_funcionario
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::empresa_status, ?)
                        """;

        KeyHolder key_holder = new GeneratedKeyHolder();

        jdbc_template.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    int idx = 1;
                    ps.setString(idx++, empresa.razao_social());
                    ps.setString(idx++, empresa.nome_fantasia());
                    ps.setString(idx++, empresa.cnpj());
                    ps.setString(idx++, empresa.telefone());
                    ps.setString(idx++, empresa.email());
                    ps.setString(idx++, empresa.endereco());
                    ps.setString(idx++, empresa.cep());
                    ps.setString(idx++, empresa.numero());
                    ps.setString(idx++, empresa.complemento());
                    ps.setString(
                            idx,
                            empresa.status() == null ? Empresa.Status.ATIVO.name() : empresa.status().name());
                    ps.setLong(idx++, getFuncionarioIdLogado());
                    return ps;
                },
                key_holder);

        Long generated_id = key_holder.getKey() != null ? key_holder.getKey().longValue() : null;

        if (generated_id == null)
            throw new IllegalStateException("Não foi possível obter o id da empresa criada");

        return findByIdNomeOuCnpj(generated_id, null, Pageable.ofSize(1))
                .getContent()
                .getFirst();
    }

    public Empresa update(Empresa empresa) {
        String sql =
                """
                        UPDATE empresa SET
                            razao_social = ?,
                            nome_fantasia = ?,
                            cnpj = ?,
                            telefone = ?,
                            email = ?,
                            endereco = ?,
                            cep = ?,
                            numero = ?,
                            complemento = ?,
                            pais = ?,
                            estado = ?,
                            municipio = ?,
                            bairro = ?,
                            tipo_empresa = ?,
                            status = ?::empresa_status
                        WHERE id = ?
                        """;

        String status = empresa.status() == null ? Empresa.Status.ATIVO.name() : empresa.status().name();

        jdbc_template.update(
                sql,
                empresa.razao_social(),
                empresa.nome_fantasia(),
                empresa.cnpj(),
                empresa.telefone(),
                empresa.email(),
                empresa.endereco(),
                empresa.cep(),
                empresa.numero(),
                empresa.complemento(),
                empresa.pais(),
                empresa.estado(),
                empresa.municipio(),
                empresa.bairro(),
                empresa.tipo_empresa(),
                status,
                empresa.id());

        return findByIdNomeOuCnpj(empresa.id(), null, Pageable.ofSize(1))
                .getContent()
                .getFirst();
    }

    public List<Pessoa> findPessoasByEmpresaId(Long empresa_id) {
        String sql =
                """
                SELECT
                    p.id,
                    p.data_hora_registro AS data_hora_registro,
                    p.data_nascimento,
                    p.nome,
                    p.cpf,
                    p.rg,
                    p.email,
                    p.telefone,
                    p.pais,
                    p.estado,
                    p.municipio,
                    p.endereco,
                    p.complemento,
                    p.vezes_hospedado,
                    p.cep,
                    p.idade,
                    p.bairro,
                    p.sexo,
                    p.numero,
                    p.status,
                    f.id AS funcionario_id,
                    pf.nome AS funcionario_nome,
                    t.id AS titular_id,
                    t.nome AS titular_nome
                FROM empresa_pessoa ep
                JOIN pessoa p ON p.id = ep.fk_pessoa
                LEFT JOIN funcionario f ON f.id = p.fk_funcionario
                LEFT JOIN pessoa pf ON pf.id = f.fk_pessoa
                LEFT JOIN pessoa t ON t.id = p.fk_titular
                WHERE ep.fk_empresa = ?
                ORDER BY p.nome
                """;

        return jdbc_template.query(sql, Pessoa.ROW_MAPPER, empresa_id);
    }


    public void vincularPessoa(Empresa.Vincular vinculo) {
        String sql =
                Boolean.TRUE.equals(vinculo.ativo())
                        ? "INSERT INTO empresa_pessoa (fk_empresa, fk_pessoa) VALUES (?, ?) ON CONFLICT DO NOTHING"
                        : "DELETE FROM empresa_pessoa WHERE fk_empresa = ? AND fk_pessoa = ?";

        jdbc_template.update(sql, vinculo.empresa().id(), vinculo.pessoa().id());
    }

    public Long getFuncionarioIdLogado(){
        return pessoa_repository.getFuncionarioPessoaIdFromRequest();
    }


}
