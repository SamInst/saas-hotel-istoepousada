-- auto-generated definition
create table pagamento
(
    id                 uuid      default gen_random_uuid() not null
        primary key,
    fk_tipo_pagamento  bigint                              not null
        constraint fk_pagamento_tipo
            references tipo_pagamento,
    fk_funcionario     bigint,
    data_hora_registro timestamp default CURRENT_TIMESTAMP not null,
    nome_pagador       varchar(255),
    descricao          varchar(255),
    valor              real      default 0                 not null,
    cancelado          boolean   default false             not null
);

alter table pagamento
    owner to neondb_owner;

