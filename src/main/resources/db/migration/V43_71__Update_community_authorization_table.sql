alter table if exists "community_authorization"
    add column if not exists email varchar unique;

alter table if exists "community_authorization"
    add column if not exists is_revoked boolean not null default false;
