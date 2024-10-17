CREATE OR REPLACE FUNCTION get_detectable_object_configuration(zdj_id VARCHAR)
    RETURNS detectable_object_configuration LANGUAGE SQL IMMUTABLE RETURNS NULL ON NULL INPUT AS $$
        SELECT * FROM detectable_object_configuration WHERE detection_job_id = zdj_id;
$$;


CREATE OR REPLACE FUNCTION get_tiles_without_detected_object(zdj_id VARCHAR)
    RETURNS SETOF detected_tile
    LANGUAGE plpgsql AS $$
        BEGIN
            RETURN QUERY
                SELECT dtt.* FROM detected_tile dtt WHERE dtt.zdj_job_id = zdj_id AND dtt.id NOT IN (SELECT dto.detected_tile_id FROM detected_object dto);
        END;
$$;

CREATE OR REPLACE FUNCTION get_in_doubt_detected_tiles(zdjId VARCHAR, is_greater BOOLEAN)
    RETURNS SETOF detected_tile
        LANGUAGE plpgsql AS $$
        DECLARE
            reference_confidence NUMERIC;
        BEGIN
            SELECT min_confidence_for_detection INTO reference_confidence FROM get_detectable_object_configuration(zdjId);

            IF reference_confidence IS NULL THEN
                            RAISE EXCEPTION 'No configured min confidence delivery found for detection job id: %', zdjId;
            END IF;

            RETURN QUERY
                SELECT dtt.* FROM detected_tile dtt
                     INNER JOIN detected_object dto ON dto.detected_tile_id = dtt.id
                     INNER JOIN detectable_object_type dtobt ON dtobt.id = dto.detected_object_type_id
                     WHERE dtt.zdj_job_id = zdjId AND ((is_greater AND dto.computed_confidence > reference_confidence) OR
                                                       (NOT is_greater AND reference_confidence >= dto.computed_confidence));
        END;
$$;