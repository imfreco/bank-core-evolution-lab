create table accounts (
    id uuid primary key,
    account_number varchar(32) not null,
    customer_id uuid not null references customers(id),
    type varchar(20) not null,
    status varchar(20) not null,
    currency varchar(3) not null,
    accounting_balance numeric(19, 2) not null,
    available_balance numeric(19, 2) not null,
    version bigint not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_accounts_account_number unique (account_number),
    constraint chk_accounts_accounting_balance_non_negative check (accounting_balance >= 0),
    constraint chk_accounts_available_balance_non_negative check (available_balance >= 0)
);

create index idx_accounts_customer_id on accounts(customer_id);
