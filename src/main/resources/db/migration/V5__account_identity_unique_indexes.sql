do $$
begin
    if exists (
        select 1
        from fp_100
        where email is not null and btrim(email) <> ''
        group by lower(btrim(email))
        having count(*) > 1
    ) then
        raise exception 'Cannot create normalized email unique index: duplicate fp_100.email values exist.';
    end if;

    if exists (
        select 1
        from fp_100
        where nick_name is not null and btrim(nick_name) <> ''
        group by btrim(nick_name)
        having count(*) > 1
    ) then
        raise exception 'Cannot create nickname unique index: duplicate fp_100.nick_name values exist.';
    end if;
end
$$;

update fp_100
set email = null
where email is not null and btrim(email) = '';

update fp_100
set email = lower(btrim(email))
where email is not null;

update fp_100
set nick_name = null
where nick_name is not null and btrim(nick_name) = '';

update fp_100
set nick_name = btrim(nick_name)
where nick_name is not null;

create unique index if not exists uq_fp_100_email_normalized
    on fp_100 (lower(btrim(email)))
    where email is not null;

create unique index if not exists uq_fp_100_nickname_normalized
    on fp_100 (btrim(nick_name))
    where nick_name is not null;
