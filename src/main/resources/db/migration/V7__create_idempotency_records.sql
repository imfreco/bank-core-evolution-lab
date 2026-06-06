create table idempotency_records (
    id uuid primary key,
    idempotency_key varchar(120) not null,
    request_hash varchar(128) not null,
    response_body text,
    operation_type varchar(80) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    constraint uk_idempotency_records_key unique (idempotency_key)
);
