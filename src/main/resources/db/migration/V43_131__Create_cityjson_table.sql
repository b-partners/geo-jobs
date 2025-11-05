create table if not exists "cityjson"
(
    id                 varchar primary key,
    s3_file_key        varchar not null,
    creation_datetime timestamp with time zone default now()::timestamp with time zone,
    detection_id varchar references "detection"("id")
);
