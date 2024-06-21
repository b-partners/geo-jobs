create or replace function remove_duplicate_bucket_paths()
    returns void
    language plpgsql
as $$
begin
    -- Delete duplicates while keeping one record per group
    delete from "detected_tile"
    where id in (
        select id
        from (
                 select id, job_id, bucket_path, row_number() over (partition by job_id, bucket_path order by id) as row_number
                 from "detected_tile"
             ) as tmp
        where row_number > 1
    );
END $$;