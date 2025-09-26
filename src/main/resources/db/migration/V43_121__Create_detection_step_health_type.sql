DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'detection_step_health') THEN
            CREATE TYPE detection_step_health AS ENUM (
                'SUCCEEDED',
                'FAILED',
                'UNKNOWN',
                'RETRYING'
                );
        END IF;
    END
$$;