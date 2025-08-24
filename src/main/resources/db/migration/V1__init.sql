create extension if not exists btree_gist;

create table buildings
(
  id          uuid               not null primary key,
  name        varchar(16) unique not null,
  description varchar(255),
  address     varchar(128)       not null,
  state       varchar(16)        not null
);

create table rooms
(
  id              uuid                  not null primary key,
  building_id     uuid                  not null,
  name            varchar(255)
    constraint unique_rooms_name unique not null,
  characteristics jsonb                 not null,
  constraint fk_rooms_on_buildings foreign key (building_id) references buildings (id)
);

create table bookings
(
  id                uuid                        not null primary key,
  end_time          timestamp(6) with time zone not null,
  start_time        timestamp(6) with time zone not null,
  lecturer_ids      UUID[],
  student_group_ids UUID[],
  room_id           uuid                        not null
    constraint fk_bookings_on_rooms references rooms (id),
  exclude using gist (room_id WITH =, tstzrange(start_time, end_time, '[)') WITH &&)
);

create index idx_bookings_start_time on bookings using brin (start_time, end_time);

