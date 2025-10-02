DO
$$
    BEGIN
        -- 1. Creates the new type if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM pg_type WHERE typname = 'detection_step_name_new'
        ) THEN
            CREATE TYPE detection_step_name_new AS ENUM (
                'REQUEST_ACCEPTED',
                'TILING',
                'MACHINE_DETECTION',
                'POST_PROCESSING'
                );
        END IF;

        -- 2. Corrects old values
        UPDATE detection_step
        SET name = 'POST_PROCESSING'
        WHERE name IN ('HUMAN_DETECTION', 'GEO_JSON_CONVERSION');

        -- 3. Verifies if the column is in the old type
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'detection_step'
              AND column_name = 'name'
              AND udt_name = 'detection_step_name'
        ) THEN
            ALTER TABLE detection_step
                ALTER COLUMN name TYPE detection_step_name_new
                    USING name::text::detection_step_name_new;
        END IF;

        -- 4. Deletes the old type if it exists
        IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'detection_step_name') THEN
            DROP TYPE detection_step_name;
        END IF;

        -- 5. Renames the new type same as the old one
        IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'detection_step_name_new') THEN
            ALTER TYPE detection_step_name_new RENAME TO detection_step_name;
        END IF;
    END
$$;
