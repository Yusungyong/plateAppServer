create table if not exists fp_500 (
  id bigserial primary key,
  author_id integer not null references fp_100(user_id),
  title varchar(255) not null,
  slug varchar(255),
  summary varchar(500),
  content text,
  servings integer,
  cook_time_min integer,
  difficulty varchar(10),
  thumbnail_url varchar(1024),
  cover_url varchar(1024),
  view_count integer not null default 0,
  like_count integer not null default 0,
  is_published boolean not null default true,
  published_at timestamp,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create unique index if not exists uq_fp_500_slug on fp_500 (slug);
create index if not exists idx_fp_500_created_at on fp_500 (created_at);
create index if not exists idx_fp_500_published_at on fp_500 (published_at);
create index if not exists idx_fp_500_pub_created on fp_500 (is_published, created_at);
create index if not exists idx_fp_500_view_count on fp_500 (view_count);
create index if not exists idx_fp_500_like_count on fp_500 (like_count);
create index if not exists idx_fp_500_difficulty on fp_500 (difficulty);

alter table fp_500
  add constraint ck_fp_500_difficulty
  check (difficulty in ('EASY', 'MEDIUM', 'HARD'));

create table if not exists fp_501 (
  id bigserial primary key,
  name varchar(100) not null,
  sort_order integer not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create unique index if not exists uq_fp_501_name on fp_501 (name);

create table if not exists fp_502 (
  recipe_id bigint not null references fp_500(id) on delete cascade,
  category_id bigint not null references fp_501(id) on delete cascade,
  created_at timestamp not null default current_timestamp,
  primary key (recipe_id, category_id)
);

create index if not exists idx_fp_502_category_recipe on fp_502 (category_id, recipe_id);
create index if not exists idx_fp_502_recipe_category on fp_502 (recipe_id, category_id);

create table if not exists fp_503 (
  id bigserial primary key,
  recipe_id bigint not null references fp_500(id) on delete cascade,
  step_no integer not null,
  title varchar(255),
  description text not null,
  image_url varchar(1024)
);

create unique index if not exists uq_fp_503_recipe_step on fp_503 (recipe_id, step_no);
create index if not exists idx_fp_503_recipe_step on fp_503 (recipe_id, step_no);

create table if not exists fp_504 (
  id bigserial primary key,
  recipe_id bigint not null references fp_500(id) on delete cascade,
  name varchar(255) not null,
  quantity varchar(100),
  created_at timestamp not null default current_timestamp
);

create index if not exists idx_fp_504_recipe on fp_504 (recipe_id);

create table if not exists fp_505 (
  id bigserial primary key,
  name varchar(100) not null,
  created_at timestamp not null default current_timestamp
);

create unique index if not exists uq_fp_505_name on fp_505 (name);

create table if not exists fp_506 (
  recipe_id bigint not null references fp_500(id) on delete cascade,
  tag_id bigint not null references fp_505(id) on delete cascade,
  created_at timestamp not null default current_timestamp,
  primary key (recipe_id, tag_id)
);

create index if not exists idx_fp_506_tag_recipe on fp_506 (tag_id, recipe_id);
create index if not exists idx_fp_506_recipe_tag on fp_506 (recipe_id, tag_id);

create table if not exists fp_507 (
  id bigserial primary key,
  recipe_id bigint not null references fp_500(id) on delete cascade,
  user_id integer not null references fp_100(user_id) on delete cascade,
  created_at timestamp not null default current_timestamp,
  unique (recipe_id, user_id)
);

create index if not exists idx_fp_507_user on fp_507 (user_id);
