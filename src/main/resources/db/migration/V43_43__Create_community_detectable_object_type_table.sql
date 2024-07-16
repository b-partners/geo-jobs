create table community_detectable_object_type(
    id varchar primary key default uuid_generate_v4(),
    type detectable_type not null,
    id_community_authorization varchar not null,
    constraint id_community_authorization_fk foreign key (id_community_authorization) references "community_authorization" (id)
);