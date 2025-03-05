create table if not exists area_picture (
    id varchar primary key,
    address varchar not null,
    zoom jsonb not null,
    file_key varchar not null,
    is_extended boolean default false,
    created_at timestamp with time zone default now(),
    community_id varchar,
    current_tile jsonb not null,
    geo_position jsonb,
    available_layers jsonb
);