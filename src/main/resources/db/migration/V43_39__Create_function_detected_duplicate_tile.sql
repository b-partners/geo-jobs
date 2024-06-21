create function get_duplicate_bucket_paths(job_id_param character varying)
    returns table(bucket_path character varying, duplicate_count integer)
    language plpgsql
as
$$
begin
    return query
        select dt.id, dt.bucket_path, count(dt.bucket_path)::integer as duplicate_count
        from "detected_tile" dt
        where dt.job_id = job_id_param
        group by dt.bucket_path
        having count(dt.bucket_path) > 1;
END;
$$;
