create extension if not exists btree_gist;

create table rooms
(
  id              uuid                  not null primary key,
  located_at      varchar(255)          not null,
  name            varchar(255)
    constraint unique_rooms_name unique not null,
  characteristics jsonb                 not null
);

create table bookings
(
  id         uuid                        not null primary key,
  end_time   timestamp(6) with time zone not null,
  start_time timestamp(6) with time zone not null,
  room_id    uuid                        not null
    constraint fk_bookings_on_rooms references rooms (id),
  exclude using gist (room_id WITH =, tstzrange(start_time, end_time, '[)') WITH &&)
);

create index idx_bookings_start_time on bookings using brin (start_time, end_time);

