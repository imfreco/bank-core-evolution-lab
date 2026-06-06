create table outbox_events (
    id uuid primary key,
    aggregate_type varchar(80) not null,
    aggregate_id varchar(80) not null,
    event_type varchar(120) not null,
    payload text not null,
    status varchar(20) not null,
    retry_count integer not null,
    created_at timestamp with time zone not null,
    published_at timestamp with time zone
);

create index idx_outbox_events_status_created_at on outbox_events(status, created_at);
