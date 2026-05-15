package saas.hotel.istoepousada.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Hospedagem;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.dto.Reserva;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.service.CalcularPrecoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class HospedagemRepository {
    private final JdbcTemplate jdbcTemplate;
    private final CalcularPrecoService calcularPrecoService;
    private final PessoaRepository pessoaRepository;

    public HospedagemRepository(JdbcTemplate jdbcTemplate, CalcularPrecoService calcularPrecoService, PessoaRepository pessoaRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.calcularPrecoService = calcularPrecoService;
        this.pessoaRepository = pessoaRepository;
    }

    private static final String SELECT_HOSPEDAGEM =
            """
                    SELECT
                        hospedagem.id                      AS hospedagem_id,
                        hospedagem.status                  AS hospedagem_status,
                        hospedagem.data_hora_registro      AS hospedagem_data_hora_registro,
                        hospedagem.data_hora_checkin       AS hospedagem_data_hora_checkin,
                        hospedagem.data_hora_checkout      AS hospedagem_data_hora_checout,
                        hospedagem.valor_total             AS hospedagem_valor_total,
                        pessoa_funcionario.id              AS hospedagem_funcionario_id,
                        pessoa_funcionario_hospedagem.nome AS hospedagem_funcionario_nome
                    FROM public.hospedagem
                    LEFT JOIN public.funcionario pessoa_funcionario ON pessoa_funcionario.id = hospedagem.fk_funcionario
                    LEFT JOIN public.pessoa pessoa_funcionario_hospedagem ON pessoa_funcionario_hospedagem.id = pessoa_funcionario.fk_pessoa
                    """;

    public Hospedagem buscar(Hospedagem.Status status, LocalDate data, Integer mes, Integer ano, String nomeTitular) {
        return null;
    }

    public Hospedagem buscarPorId(Long id) {
        try {
            return jdbcTemplate.queryForObject(SELECT_HOSPEDAGEM + " WHERE hospedagem.id = ? ",
                    Hospedagem.MAPPER, id
            );
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Hospedagem nao encontrada para o id: " + id);
        }
    }

    public void salvar(Hospedagem.HospedagemRequest request) {
        isQuartoDisponivel(
                request.quarto_id(),
                request.data_hora_checkin(),
                request.data_hora_checkout(),
                null
        );
        var hospedagem_id = jdbcTemplate.queryForObject("""
            insert into hospedagem (
                                    fk_funcionario,
                                    data_hora_registro,
                                    data_hora_checkin,
                                    data_hora_checkout,
                                    valor_total,
                                    status,
                                    observacao)
            values (?, now(), ?, ?, ?, ?, ?)
            returning id;
        """,
                Long.class,
                getFuncionarioId(),
                request.data_hora_checkin(),
                request.data_hora_checkout());
        switch (request.status()) {
            case ORCAMENTO: {
            }
            case RESERVA_SOLICITADA: {

            }
            case RESERVA_ATIVA: {
            }
            case PERNOITE_ATIVA: {
                List<Hospedagem.Diaria.Request> diarias = new ArrayList<>(
                        List.of(new Hospedagem.Diaria.Request(
                                request.quarto_id(),
                                request.data_hora_checkin(),
                                request.data_hora_checkout(),
                                request.pessoas(),
                                null)));

                adicionarDiarias(hospedagem_id, diarias);
                adicionarPessoas(hospedagem_id, request.pessoas());
                alterarStatus(hospedagem_id, Hospedagem.Status.PERNOITE_ATIVA);
            }
            case DAY_USE_ATIVA: {
            }
        }
    }

    public void alterar(Hospedagem.HospedagemUpdate request) {
        Hospedagem hospedagem = buscarPorId(request.id());

        validarTransicaoDeStatus(hospedagem.status(), request.status());
        switch (request.status()) {
            case ORCAMENTO_CANCELADO: {
                alterarStatus(request.id(), Hospedagem.Status.ORCAMENTO_CANCELADO);
            }
            case RESERVA_ATIVA: {
                alterarStatus(request.id(), Hospedagem.Status.RESERVA_ATIVA);
            }
            case RESERVA_CANCELADA: {
                alterarStatus(request.id(), Hospedagem.Status.RESERVA_CANCELADA);
            }
            case RESERVA_AUSENTE: {
                alterarStatus(request.id(), Hospedagem.Status.RESERVA_AUSENTE);
            }
            case PERNOITE_ATIVA: {
                List<Hospedagem.Diaria.Request> diarias = new ArrayList<>(
                        List.of(new Hospedagem.Diaria.Request(
                                request.quarto_id(),
                                request.data_hora_checkin(),
                                request.data_hora_checkout(),
                                request.pessoas(),
                                null)));
                adicionarDiarias(request.id(), diarias);
                adicionarPessoas(request.id(), request.pessoas());
                alterarStatus(request.id(), Hospedagem.Status.PERNOITE_ATIVA);
            }
            case PERNOITE_CANCELADA: {
                removerDiarias(request.id());
                alterarStatus(request.id(), Hospedagem.Status.PERNOITE_CANCELADA);
            }
            case PERNOITE_FINALIZADA: {
                alterarStatus(request.id(), Hospedagem.Status.PERNOITE_FINALIZADA);
            }
            case PERNOITE_FINALIZADA_PAGAMENTO_PENDENTE: {
                alterarStatus(request.id(), Hospedagem.Status.PERNOITE_FINALIZADA_PAGAMENTO_PENDENTE);
            }
//            case DAY_USE_SOLICITADA: {
//            }
//            case DAY_USE_ATIVA: {
//            }
//            case DAY_USE_CANCELADA: {
//            }
//            case DAY_USE_FINALIZADA: {
//            }
//            case DAY_USE_FINALIZADA_PAGAMENTO_PENDENTE: {
//            }
//            case DAY_USE_AUSENTE: {
//                alterarStatus(request.id(), Hospedagem.Status.DAY_USE_AUSENTE);
//            }
        }
    }

    public void alterarStatus(Long hospedagemId, Hospedagem.Status status) {
        jdbcTemplate.update("""
                update hospedagem set status = ?::hospedagem_status where id = ?
                """);
    }

    public void excluir(Long id) {
    }

    public void adicionarOrcamento() {
    }

    public void cancelarOrcamento() {
    }

    public void adicionarPagamentos() {
    }

    public void editarPagamentos() {
    }

    public void excluirPagamentos() {
    }

    private List<Long> filtrarPessoasDuplicadas(Long hospedagemId, List<Long> pessoasIds) {
        List<Long> existentes = jdbcTemplate.queryForList("""
            SELECT pessoa_id FROM hospedagem_pessoa
            WHERE hospedagem_id = ?
              AND pessoa_id IN (%s)
            """.formatted(pessoasIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))),
                Long.class,
                hospedagemId);

        return pessoasIds.stream()
                .filter(id -> !existentes.contains(id))
                .toList();
    }

    public void adicionarPessoas(Long hospedagemId, List<Long> pessoasIds) {
        List<Long> pessoasPendentes = filtrarPessoasDuplicadas(hospedagemId, pessoasIds);

        if (pessoasPendentes.isEmpty()) return;

        AtomicInteger index = new AtomicInteger(0);

        jdbcTemplate.batchUpdate("""
            INSERT INTO hospedagem_pessoa(hospedagem_id, pessoa_id, representante)
            VALUES (?, ?, ?)
            """,
                pessoasPendentes,
                pessoasPendentes.size(),
                (ps, pessoaId) -> {
                    ps.setLong(1, hospedagemId);
                    ps.setLong(2, pessoaId);
                    ps.setBoolean(3, index.getAndIncrement() == 0);
                });
    }

    public void removerPessoas(Long hospedagemId, List<Long> pessoasIds) {
        List<Long> pessoasExistentes = filtrarPessoasExistentes(hospedagemId, pessoasIds);

        if (pessoasExistentes.isEmpty()) return;

        jdbcTemplate.batchUpdate("""
            DELETE FROM hospedagem_pessoa
            WHERE hospedagem_id = ?
              AND pessoa_id = ?
            """,
                pessoasExistentes,
                pessoasExistentes.size(),
                (ps, pessoaId) -> {
                    ps.setLong(1, hospedagemId);
                    ps.setLong(2, pessoaId);
                });
    }

    private List<Long> filtrarPessoasExistentes(Long hospedagemId, List<Long> pessoasIds) {
        return jdbcTemplate.queryForList("""
            SELECT pessoa_id FROM hospedagem_pessoa
            WHERE hospedagem_id = ?
              AND pessoa_id IN (%s)
            """.formatted(pessoasIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))),
                Long.class,
                hospedagemId);
    }


//    public void adicionarConsumos(Long hospedagemId, Hospedagem.) {
//    }

    public void editarConsumos() {
    }

    public void removerConsumos() {
    }

    public void adicionarDiarias(Long hospedagemId, List<Hospedagem.Diaria.Request> diarias) {
        buscarPorId(hospedagemId);

        List<Reserva.CalcularPrecoRequest> calcularPrecoRequests = new ArrayList<>();

        List<Hospedagem.Diaria.Request> diariasNaoCadastradas = new ArrayList<>();

        diarias.forEach(diaria -> {
            log.info("Adicionando diarias para a hospedagem {} e quarto {}", hospedagemId, diaria.quarto_id());
            if (isDiariaJaExiste(hospedagemId, diaria)) {
                log.info("Diária [{}>{}] já existe para a hospedagem {} e quarto {}",
                        diaria.checkin(),
                        diaria.checkout(),
                        hospedagemId,
                        diaria.quarto_id()
                );
                return;
            }
            if (!isQuartoDisponivel(diaria.quarto_id(), diaria.checkin(), diaria.checkout(), hospedagemId)) {
                log.info("Diaria: [{}>{}] nao disponivel para o quarto: {}. O apartamento se encontra indisponível ou ocupado por outra hospedagem.",
                        diaria.checkin(),
                        diaria.checkout(),
                        diaria.quarto_id()
                );
                throw new IllegalStateException("Diaria: [" + diaria.checkin() + ">"
                        + diaria.checkout() + "] nao disponivel para o quarto: "
                        + diaria.quarto_id()
                        + ". O apartamento se encontra indisponivel ou ocupado por outra hospedagem.");
            }

            List<LocalDate> datasNascimento = new ArrayList<>();

            diaria.pessoas().forEach(pessoaId -> {
                Pessoa pessoa = pessoaRepository.findById(pessoaId);
                datasNascimento.add(pessoa.data_nascimento());
                log.info("Adicionando pessoa {} para a hospedagem {} e quarto {}", pessoaId, hospedagemId, diaria.quarto_id());
            });

            calcularPrecoRequests.add(new Reserva.CalcularPrecoRequest(
                    diaria.quarto_id(),
                    diaria.checkin().toLocalDate(),
                    diaria.checkout().toLocalDate(),
                    datasNascimento,
                    null,
                    null
            ));
            diariasNaoCadastradas.add(diaria);
        });

        var resultadoCalculo = calcularPrecoService.calcularPreco(calcularPrecoRequests);

        jdbcTemplate.batchUpdate("""
                        INSERT INTO diaria (fk_hospedagem, fk_quarto, numero, checkin, checkout, valor)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                diariasNaoCadastradas,
                diariasNaoCadastradas.size(),
                (ps, d) -> {
                    ps.setLong(1, hospedagemId);
                    ps.setLong(2, d.quarto_id());
                    ps.setInt(3, diariasNaoCadastradas.indexOf(d) + 1);
                    ps.setObject(4, d.checkin());
                    ps.setObject(5, d.checkout());
                    ps.setDouble(6, resultadoCalculo.getFirst().valor_total());
                });
    }

    private Boolean isDiariaJaExiste(Long hospedagemId, Hospedagem.Diaria.Request diaria) {
        return jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                                       SELECT 1
                                       FROM diaria
                                       WHERE fk_hospedagem = ?
                                         AND fk_quarto = ?
                                         AND checkin = ?
                                         AND checkout = ?
                                   )
                """, Boolean.class, hospedagemId, diaria.quarto_id(), diaria.checkin(), diaria.checkout());
    }

    public List<Hospedagem.Diaria> listarDiarias(Long hospedagemId) {
        return jdbcTemplate.query("""
                        SELECT
                             diaria.id                          AS diaria_id,
                             diaria.numero                      AS diaria_numero,
                             diaria.valor                       AS diaria_valor,
                             diaria.checkin                     AS diaria_checkin,
                             diaria.checkout                    AS diaria_checkout,
                             quarto.id                          AS diaria_quarto_id,
                             quarto.descricao                   AS diaria_quarto_descricao
                         FROM public.diaria
                         LEFT JOIN public.quarto quarto ON quarto.id = diaria.fk_quarto
                         WHERE fk_hospedagem = ?;
                        """, (rs, x) -> {
                    Long quartoId = rs.getObject("diaria_quarto_id", Long.class);
                    return new Hospedagem.Diaria(
                            rs.getLong("diaria_id"),
                            rs.getInt("diaria_numero"),
                            quartoId == null
                                    ? null
                                    : new Quarto.Descricao(quartoId, rs.getString("diaria_quarto_descricao")),
                            rs.getObject("diaria_checkin", LocalDateTime.class),
                            rs.getObject("diaria_checkout", LocalDateTime.class),
                            rs.getFloat("diaria_valor"),
                            null);
                },
                hospedagemId
        );
    }

    public void removerDiarias(Long id) {
        jdbcTemplate.update("DELETE FROM diaria WHERE id = ?", id);
        log.info("Removendo diárias para a hospedagem {}", id);
    }

    public Boolean isQuartoDisponivel(
            Long quartoId,
            LocalDateTime checkin,
            LocalDateTime checkout,
            Long hospedagemIdExcluido) {
        log.info("Validando disponibilidade do quarto {} para checkin {} e checkout {}", quartoId, checkin, checkout);

        Quarto.Status status = jdbcTemplate.queryForObject(
                "SELECT status FROM public.quarto WHERE id = ?",
                Quarto.Status.class,
                quartoId
        );
        if (status != Quarto.Status.OCUPADO) return false;

        String sqlHospedagem =
                """
                        SELECT COUNT(*) > 0
                        FROM diaria
                        JOIN hospedagem ON hospedagem.id = diaria.fk_hospedagem
                        WHERE diaria.fk_quarto = 1
                          AND hospedagem.status IN (
                          'HOSPEDAGEM_ORCAMENTO',
                          'HOSPEDAGEM_RESERVA_ATIVA',
                          'HOSPEDAGEM_RESERVA_SOLICITADA',
                          'HOSPEDAGEM_PERNOITE_ATIVA',
                          'HOSPEDAGEM_DAY_USE_SOLICITADA',
                          'HOSPEDAGEM_DAY_USE_ATIVA')
                          AND diaria.checkin < '2025-05-13'
                          AND diaria.checkout > '2025-05-13'
                        """ + (hospedagemIdExcluido != null ? " AND hospedagem.id != ? " : "");

        boolean hospedagemConflito;
        if (hospedagemIdExcluido != null) {
            hospedagemConflito = jdbcTemplate.queryForObject(
                    sqlHospedagem,
                    Boolean.class,
                    quartoId,
                    checkin,
                    checkout,
                    hospedagemIdExcluido
            );
        } else {
            hospedagemConflito = jdbcTemplate.queryForObject(
                    sqlHospedagem,
                    Boolean.class,
                    quartoId,
                    checkout,
                    checkin
            );
        }
        log.info("Quarto disponivel: {}", hospedagemConflito);
        return hospedagemConflito;
    }

    public void validarTransicaoDeStatus(Hospedagem.Status anterior, Hospedagem.Status novo) {
        Map<Hospedagem.Status, Set<Hospedagem.Status>> transicoesPermitidas = Map.ofEntries(
                Map.entry(Hospedagem.Status.ORCAMENTO, EnumSet.of(
                        Hospedagem.Status.ORCAMENTO_CANCELADO,
                        Hospedagem.Status.RESERVA_SOLICITADA)),
                Map.entry(Hospedagem.Status.ORCAMENTO_CANCELADO, EnumSet.of(
                        Hospedagem.Status.ORCAMENTO)),

                Map.entry(Hospedagem.Status.RESERVA_SOLICITADA, EnumSet.of(
                        Hospedagem.Status.RESERVA_ATIVA,
                        Hospedagem.Status.RESERVA_CANCELADA)),
                Map.entry(Hospedagem.Status.RESERVA_ATIVA, EnumSet.of(
                        Hospedagem.Status.RESERVA_CANCELADA,
                        Hospedagem.Status.RESERVA_AUSENTE,
                        Hospedagem.Status.PERNOITE_ATIVA)),
                Map.entry(Hospedagem.Status.RESERVA_AUSENTE, EnumSet.of(
                        Hospedagem.Status.RESERVA_ATIVA)),

                Map.entry(Hospedagem.Status.PERNOITE_ATIVA, EnumSet.of(
                        Hospedagem.Status.PERNOITE_CANCELADA,
                        Hospedagem.Status.PERNOITE_FINALIZADA,
                        Hospedagem.Status.PERNOITE_FINALIZADA_PAGAMENTO_PENDENTE)),
                Map.entry(Hospedagem.Status.PERNOITE_FINALIZADA_PAGAMENTO_PENDENTE, EnumSet.of(
                        Hospedagem.Status.PERNOITE_FINALIZADA)),

                Map.entry(Hospedagem.Status.DAY_USE_SOLICITADA, EnumSet.of(
                        Hospedagem.Status.DAY_USE_AUSENTE,
                        Hospedagem.Status.DAY_USE_ATIVA,
                        Hospedagem.Status.DAY_USE_CANCELADA)),
                Map.entry(Hospedagem.Status.DAY_USE_ATIVA, EnumSet.of(
                        Hospedagem.Status.DAY_USE_FINALIZADA,
                        Hospedagem.Status.DAY_USE_FINALIZADA_PAGAMENTO_PENDENTE)),
                Map.entry(Hospedagem.Status.DAY_USE_FINALIZADA_PAGAMENTO_PENDENTE, EnumSet.of(
                        Hospedagem.Status.DAY_USE_FINALIZADA))
        );

        Set<Hospedagem.Status> estadosFinais = EnumSet.of(
                Hospedagem.Status.RESERVA_CANCELADA,
                Hospedagem.Status.PERNOITE_CANCELADA,
                Hospedagem.Status.PERNOITE_FINALIZADA,
                Hospedagem.Status.DAY_USE_CANCELADA,
                Hospedagem.Status.DAY_USE_FINALIZADA
        );

        if (estadosFinais.contains(anterior)) {
            throw new IllegalStateException("Status " + anterior + " é um estado final e não pode ser alterado.");
        }

        Set<Hospedagem.Status> permitidos = transicoesPermitidas.get(anterior);
        if (permitidos == null || !permitidos.contains(novo)) {
            throw new IllegalStateException("Transição de status inválida: " + anterior + " -> " + novo);
        }
    }

    public Long getFuncionarioId(){
       return pessoaRepository.getFuncionarioIdFromRequest();
    }
}
