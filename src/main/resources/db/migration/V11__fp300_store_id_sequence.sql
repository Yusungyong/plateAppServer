-- Allocate fp_300.store_id atomically. Keep the column default for non-JPA inserts;
-- Hibernate uses the same sequence through Fp300Store's @SequenceGenerator.
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '60s';

CREATE SEQUENCE IF NOT EXISTS public.fp_300_store_id_seq AS INTEGER;

ALTER SEQUENCE public.fp_300_store_id_seq OWNED BY public.fp_300.store_id;

ALTER TABLE public.fp_300
    ALTER COLUMN store_id SET DEFAULT nextval('public.fp_300_store_id_seq'::regclass);

-- is_called = false makes the next nextval() return exactly max(store_id) + 1.
SELECT setval(
    'public.fp_300_store_id_seq'::regclass,
    COALESCE((SELECT MAX(store_id) FROM public.fp_300), 0) + 1,
    false
);
