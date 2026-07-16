alter table if exists fp_400
    add column if not exists open_yn char(1);

do $$
begin
    if to_regclass('public.fp_400') is not null
       and not exists (
           select 1
             from pg_constraint
            where conname = 'ck_fp400_open_yn'
              and conrelid = 'public.fp_400'::regclass
       ) then
        alter table fp_400
            add constraint ck_fp400_open_yn
            check (open_yn is null or btrim(open_yn) in ('Y', 'N'))
            not valid;
    end if;
end $$;

comment on column fp_400.open_yn is
    'Image feed visibility: Y public, N private, NULL legacy policy unresolved';
