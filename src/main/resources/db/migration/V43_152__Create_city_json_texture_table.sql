create table if not exists "city_json_texture"
(
    id                   varchar primary key,
    city_json_request_id varchar,
    top_left_longitude   double,
    top_left_latitude    double,
    pixel_width          double,
    pixel_height         double,
    image_width          int,
    image_height         int,
    image_uri            varchar,

    constraint fk_city_json_texture_city_json_request_id
        foreign key (city_json_request_id)
            references city_json_request (id)
);