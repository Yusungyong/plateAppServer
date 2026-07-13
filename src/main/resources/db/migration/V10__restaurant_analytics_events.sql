create table if not exists restaurant_analytics_events (
    id bigserial primary key,
    restaurant_id bigint not null references restaurants(id) on delete cascade,
    event_type varchar(40) not null,
    event_uid varchar(120),
    username varchar(255),
    user_id integer,
    is_guest boolean not null default false,
    guest_id varchar(120),
    session_id varchar(150),
    device_id varchar(200),
    surface varchar(60),
    event_source varchar(60),
    menu_id bigint,
    content_type varchar(20),
    content_id bigint,
    client_event_at timestamptz,
    server_event_at timestamptz not null default now(),
    user_agent varchar(300),
    constraint ck_restaurant_analytics_event_type check (
        event_type in (
            'DETAIL_VIEW',
            'MAP_IMPRESSION',
            'SEARCH_IMPRESSION',
            'PHONE_CLICK',
            'DIRECTION_CLICK',
            'SHARE_CLICK',
            'MENU_VIEW',
            'MENU_SAVE',
            'VISIT_CONVERSION',
            'REVIEW_CONVERSION'
        )
    )
);

create unique index if not exists ux_restaurant_analytics_events_event_uid
    on restaurant_analytics_events (event_uid)
    where event_uid is not null;

create index if not exists idx_restaurant_analytics_events_store_time
    on restaurant_analytics_events (restaurant_id, server_event_at desc);

create index if not exists idx_restaurant_analytics_events_store_type_time
    on restaurant_analytics_events (restaurant_id, event_type, server_event_at desc);

create index if not exists idx_restaurant_analytics_events_menu
    on restaurant_analytics_events (restaurant_id, menu_id, server_event_at desc)
    where menu_id is not null;
