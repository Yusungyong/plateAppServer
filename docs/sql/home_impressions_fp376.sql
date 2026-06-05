create table if not exists fp_376 (
    impression_id bigserial primary key,
    user_id integer,
    username varchar(50),
    is_guest boolean not null default false,
    guest_id varchar(100),
    session_id varchar(150),
    device_id varchar(200),
    content_type varchar(20) not null,
    store_id integer,
    feed_no integer,
    surface varchar(60) not null default 'home',
    request_id varchar(100),
    position_no integer,
    client_impressed_at timestamp,
    impressed_at timestamp not null default now(),
    constraint fp_376_content_type_check
        check (content_type in ('VIDEO', 'IMAGE')),
    constraint fp_376_content_id_check
        check (
            (content_type = 'VIDEO' and store_id is not null)
            or
            (content_type = 'IMAGE' and feed_no is not null)
        )
);

create index if not exists idx_fp_376_username_video_recent
    on fp_376 (username, impressed_at desc, store_id)
    where username is not null and content_type = 'VIDEO';

create index if not exists idx_fp_376_guest_video_recent
    on fp_376 (guest_id, impressed_at desc, store_id)
    where guest_id is not null and content_type = 'VIDEO';

create index if not exists idx_fp_376_username_image_recent
    on fp_376 (username, impressed_at desc, feed_no)
    where username is not null and content_type = 'IMAGE';

create index if not exists idx_fp_376_guest_image_recent
    on fp_376 (guest_id, impressed_at desc, feed_no)
    where guest_id is not null and content_type = 'IMAGE';

create index if not exists idx_fp_376_impressed_at
    on fp_376 (impressed_at desc);
