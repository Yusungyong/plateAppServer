alter table if exists fp_300
    add column if not exists restaurant_id bigint;

create index if not exists idx_fp300_restaurant_id
    on fp_300 (restaurant_id);

do $$
begin
    if to_regclass('public.fp_300') is not null
       and to_regclass('public.restaurants') is not null
       and not exists (
           select 1
           from pg_constraint
           where conname = 'fk_fp300_restaurant'
       ) then
        alter table fp_300
            add constraint fk_fp300_restaurant
            foreign key (restaurant_id)
            references restaurants(id)
            on delete set null;
    end if;
end $$;

update fp_300 s
set restaurant_id = r.id
from restaurants r
where s.restaurant_id is null
  and s.store_id::bigint = r.id;

with exact_matches as (
    select
        s.store_id,
        min(r.id) as restaurant_id,
        count(*) as match_count
    from fp_300 s
    join restaurants r
      on lower(trim(coalesce(nullif(s.store_name, ''), s.title, ''))) = lower(trim(r.title))
     and lower(trim(coalesce(s.address, ''))) = lower(trim(r.address))
    where s.restaurant_id is null
      and coalesce(nullif(s.store_name, ''), s.title) is not null
      and s.address is not null
      and trim(coalesce(nullif(s.store_name, ''), s.title, '')) <> ''
      and trim(coalesce(s.address, '')) <> ''
    group by s.store_id
)
update fp_300 s
set restaurant_id = m.restaurant_id
from exact_matches m
where s.restaurant_id is null
  and s.store_id = m.store_id
  and m.match_count = 1;
