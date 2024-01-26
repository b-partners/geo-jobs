create table if not exists "zone_task_status"
(
    id varchar primary key default uuid_generate_v4(),
    progression progression_status not null,
    health health_status not null,
    creation_datetime timestamp without time zone not null default now()::timestamp without time zone,
    task_id varchar ,
    job_type job_type,
    message varchar
);
