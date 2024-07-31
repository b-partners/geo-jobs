alter table if exists zone_tiling_job add column if not exists end_to_end_id varchar;
alter table if exists zone_detection_job add column if not exists end_to_end_id varchar;