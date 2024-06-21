create function get_duplicate_bucket_paths(job_id_param character varying)
    returns TABLE(bucket_path character varying, duplicate_count integer)
    language plpgsql
as
$$
BEGIN
    RETURN QUERY
        SELECT dt.id, dt.bucket_path, COUNT(dt.bucket_path)::integer AS duplicate_count
        FROM "detected_tile" dt
        WHERE dt.job_id = job_id_param
        GROUP BY dt.bucket_path
        HAVING COUNT(dt.bucket_path) > 1;
END;
$$;
