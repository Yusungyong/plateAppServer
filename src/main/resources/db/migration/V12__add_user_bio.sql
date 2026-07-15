SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '60s';

ALTER TABLE public.fp_100
    ADD COLUMN IF NOT EXISTS bio varchar(200);
