create table if not exists "annotated_task" (
    id                 varchar primary key default uuid_generate_v4(),
    create_annotated_task_id  varchar,
    job_type           job_type,
    job_id             varchar,
    as_job_id             varchar,
    submission_instant timestamp with time zone not null default now()::timestamp with time zone
)