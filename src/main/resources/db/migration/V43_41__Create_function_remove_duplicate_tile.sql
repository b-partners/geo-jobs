create or replace function remove_duplicate_bucket_paths() returns void
    language plpgsql
as
$$
declare
    tile record;
    duplicate record;
begin
    -- Browse each unique tuple of (bucket_path, job_id) that repeats
    for tile in
        select bucket_path, job_id, MIN(id) as id
        from detected_tile
        group by bucket_path, job_id
        having COUNT(*) > 1
        loop
            -- For each duplication, except the first (MIN(id))
            for duplicate in
                select id
                from detected_tile
                where bucket_path = tile.bucket_path and job_id = tile.job_id and id <> tile.id
                loop
                    -- Update references in detected_object
                    update detected_object
                    set detected_tile_id = tile.id
                    where detected_tile_id = duplicate.id;

                    -- Delete duplication
                    delete from detected_tile
                    where id = duplicate.id;
                end loop ;
        end loop ;
end ;
$$;

