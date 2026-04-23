alter table if exists annotation_delivery_job add column if not exists
    is_integration_test boolean;
alter table if exists annotation_delivery_task add column if not exists
    is_integration_test boolean;
alter table if exists annotation_retrieving_job add column if not exists
    is_integration_test boolean;
alter table if exists annotation_retrieving_task add column if not exists
    is_integration_test boolean;
alter table if exists detection_address_conversion_job add column if not exists
    is_integration_test boolean;
alter table if exists detection_address_conversion_task add column if not exists
    is_integration_test boolean;
alter table if exists geo_json_conversion_job add column if not exists
    is_integration_test boolean;
alter table if exists geo_json_conversion_task add column if not exists
    is_integration_test boolean;
alter table if exists job_status add column if not exists
    is_integration_test boolean;
alter table if exists parcel_detection_job add column if not exists
    is_integration_test boolean;
alter table if exists parcel_detection_task add column if not exists
    is_integration_test boolean;
alter table if exists parcel_tiling_task add column if not exists
    is_integration_test boolean;
alter table if exists parcel_with_tiling_task add column if not exists
    is_integration_test boolean;
alter table if exists parcel_with_detection_task add column if not exists
    is_integration_test boolean;
alter table if exists tiling_task add column if not exists
    is_integration_test boolean;
alter table if exists zone_tiling_job add column if not exists
    is_integration_test boolean;
alter table if exists zone_detection_job add column if not exists
    is_integration_test boolean;
alter table if exists tile_detection_task add column if not exists
    is_integration_test boolean;