create index if not exists community_authorization_api_key_community_owner_id_idx
    on community_authorization_api_key (community_owner_id);

create index if not exists revoked_api_key_community_owner_id_idx
    on revoked_api_key (community_owner_id);