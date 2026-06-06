create table audit_logs (
    id uuid primary key,
    operation_type varchar(80) not null,
    entity_type varchar(80) not null,
    entity_id varchar(80) not null,
    actor varchar(120) not null,
    channel varchar(40) not null,
    correlation_id varchar(120),
    details text not null,
    created_at timestamp with time zone not null
);

create index idx_audit_logs_entity on audit_logs(entity_type, entity_id);
