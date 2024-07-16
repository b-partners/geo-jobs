create table community_used_surface(
    id varchar primary key default uuid_generate_v4(),
    used_surface double check (used_surface >= 0),
    usage_datetime timestamp without time zone not null default now()::timestamp without time zone,
    id_community_authorization varchar not null,
    constraint id_community_authorization_fk foreign key (id_community_authorization) references "community_authorization" (id)
);
