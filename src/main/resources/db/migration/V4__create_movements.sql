create table movements (
    id uuid primary key,
    account_id uuid not null references accounts(id),
    transfer_id uuid not null references transfers(id),
    type varchar(20) not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    balance_after_movement numeric(19, 2) not null,
    description varchar(250) not null,
    created_at timestamp with time zone not null,
    constraint chk_movements_amount_positive check (amount > 0)
);

create index idx_movements_account_id_created_at on movements(account_id, created_at);
