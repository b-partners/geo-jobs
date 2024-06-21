CREATE or replace FUNCTION remove_duplicate_bucket_paths()
    RETURNS VOID
    LANGUAGE plpgsql
AS $$
BEGIN
    -- Delete duplicates while keeping one record per group
    DELETE FROM "detected_tile"
    WHERE id IN (
        SELECT id
        FROM (
                 SELECT id, job_id, bucket_path, ROW_NUMBER() OVER (PARTITION BY job_id, bucket_path ORDER BY id) AS rn
                 FROM "detected_tile"
             ) AS tmp
        WHERE rn > 1
    );
END $$;