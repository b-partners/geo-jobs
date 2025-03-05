create table if not exists area_picture_map_layer (
    id varchar primary key,
    source jsonb,
    year int,
    name varchar,
    department_name varchar,
    max_zoom_level jsonb,
    precision_level_in_cm int
);