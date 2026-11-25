alter table community_authorization
    add column if not exists dashboard_user_id varchar;
create unique index if not exists community_authorization_dashboard_user_id_uindex
    on community_authorization (dashboard_user_id) where dashboard_user_id is not null;
