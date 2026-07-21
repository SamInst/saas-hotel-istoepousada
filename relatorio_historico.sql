-- Histórico de alterações de relatórios (quem alterou e o que mudou).
-- Uma linha por evento de UPDATE no PUT /relatorio.
-- "alteracoes" guarda a lista de campos alterados: [{"campo": "...", "de": "...", "para": "..."}]

create table relatorio_historico
(
    id             bigserial                           not null
        primary key,
    fk_relatorio   bigint                              not null
        constraint fk_relatorio_historico_relatorio
            references relatorio (id) on delete cascade,
    fk_funcionario bigint
        constraint fk_relatorio_historico_funcionario
            references funcionario (id),
    acao           varchar(20) default 'UPDATE'        not null,
    data_hora      timestamp   default CURRENT_TIMESTAMP not null,
    alteracoes     jsonb                               not null
);

create index idx_relatorio_historico_relatorio
    on relatorio_historico (fk_relatorio, data_hora desc);

alter table relatorio_historico
    owner to neondb_owner;
