create table customers (
    id uuid primary key,
    document_type varchar(20) not null,
    document_number varchar(40) not null,
    full_name varchar(160) not null,
    email varchar(160) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_customers_document unique (document_type, document_number)
);
