DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'detection_step_progression') THEN
            CREATE TYPE detection_step_progression AS ENUM (
                'PENDING',
                'PROCESSING',
                'FINISHED'
                );
        END IF;
    END
$$;