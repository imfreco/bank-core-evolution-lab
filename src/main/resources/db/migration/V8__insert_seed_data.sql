insert into customers (
    id, document_type, document_number, full_name, email, status, created_at, updated_at
) values
('11111111-1111-1111-1111-111111111111', 'CC', '10000001', 'Ana Core', 'ana.core@example.com', 'ACTIVE', now(), now()),
('22222222-2222-2222-2222-222222222222', 'CC', '10000002', 'Luis Pagos', 'luis.pagos@example.com', 'ACTIVE', now(), now());

insert into accounts (
    id, account_number, customer_id, type, status, currency, accounting_balance, available_balance, version, created_at, updated_at
) values
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '1000000001', '11111111-1111-1111-1111-111111111111', 'SAVINGS', 'ACTIVE', 'COP', 1000000.00, 1000000.00, 0, now(), now()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '1000000002', '22222222-2222-2222-2222-222222222222', 'SAVINGS', 'ACTIVE', 'COP', 500000.00, 500000.00, 0, now(), now()),
('cccccccc-cccc-cccc-cccc-cccccccccccc', '1000000003', '11111111-1111-1111-1111-111111111111', 'CHECKING', 'BLOCKED', 'COP', 250000.00, 250000.00, 0, now(), now());
