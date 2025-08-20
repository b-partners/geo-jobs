create table if not exists "geo_json_continuation" (
    id varchar primary key,
    file_key varchar,
    status progression_status not null default 'PENDING'
)