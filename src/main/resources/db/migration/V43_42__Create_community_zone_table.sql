create table community_zone
(
    id   varchar primary key default uuid_generate_v4(),
    name varchar not null,
    id_community_authorization varchar not null,
    constraint id_community_authorization_fk foreign key (id_community_authorization) references "community_authorization" (id)
);
