alter table if exists fp_400
    add column if not exists restaurant_id bigint;

create index if not exists idx_fp400_restaurant_id
    on fp_400 (restaurant_id);

do $$
begin
    if to_regclass('public.fp_400') is not null
       and to_regclass('public.restaurants') is not null
       and not exists (
           select 1
           from pg_constraint
           where conname = 'fk_fp400_restaurant'
       ) then
        alter table fp_400
            add constraint fk_fp400_restaurant
            foreign key (restaurant_id)
            references restaurants(id)
            on delete set null;
    end if;
end $$;

with exact_matches as (
    select
        f.feed_no,
        min(r.id) as restaurant_id,
        count(*) as match_count
    from fp_400 f
    join restaurants r
      on lower(trim(coalesce(nullif(f.store_name, ''), f.feed_title, ''))) = lower(trim(r.title))
     and lower(trim(coalesce(f.location, ''))) = lower(trim(r.address))
    where f.restaurant_id is null
      and coalesce(nullif(f.store_name, ''), f.feed_title) is not null
      and f.location is not null
      and trim(coalesce(nullif(f.store_name, ''), f.feed_title, '')) <> ''
      and trim(coalesce(f.location, '')) <> ''
    group by f.feed_no
)
update fp_400 f
set restaurant_id = m.restaurant_id
from exact_matches m
where f.restaurant_id is null
  and f.feed_no = m.feed_no
  and m.match_count = 1;
