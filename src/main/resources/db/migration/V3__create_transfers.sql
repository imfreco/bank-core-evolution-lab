create table transfers (
    id uuid primary key,
    transfer_reference varchar(64) not null,
    source_account_id uuid not null references accounts(id),
    target_account_id uuid not null references accounts(id),
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    status varchar(20) not null,
    idempotency_key varchar(120) not null,
    failure_reason varchar(500),
    created_at timestamp with time zone not null,
    completed_at timestamp with time zone,
    constraint uk_transfers_reference unique (transfer_reference),
    constraint uk_transfers_idempotency_key unique (idempotency_key),
    constraint chk_transfers_amount_positive check (amount > 0)
);

create index idx_transfers_transfer_reference on transfers(transfer_reference);
create index idx_transfers_idempotency_key on transfers(idempotency_key);
