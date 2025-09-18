CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE buildings
(
  id          uuid         NOT NULL PRIMARY KEY,
  name        varchar(16)  NOT NULL UNIQUE,
  description varchar(255),
  address     varchar(128) NOT NULL
);

CREATE TABLE rooms
(
  id              uuid        NOT NULL PRIMARY KEY,
  building_id     uuid        NOT NULL,
  name            varchar(16) NOT NULL,
  chem_symbol     varchar(16) NOT NULL,
  characteristics jsonb       NOT NULL,
  parent_room_id  uuid        NULL,

  CONSTRAINT unique_rooms_name UNIQUE (name),
  CONSTRAINT fk_rooms_on_buildings
    FOREIGN KEY (building_id) REFERENCES buildings (id),
  CONSTRAINT fk_rooms_on_parent
    FOREIGN KEY (parent_room_id) REFERENCES rooms (id) ON DELETE SET NULL,
  CONSTRAINT chk_rooms_no_self_parent
    CHECK (parent_room_id IS NULL OR parent_room_id <> id)
);

CREATE INDEX idx_rooms_parent_room_id ON rooms (parent_room_id);

CREATE TABLE bookings
(
  id                uuid                        NOT NULL PRIMARY KEY,
  start_time        timestamp(6) with time zone NOT NULL,
  end_time          timestamp(6) with time zone NOT NULL,
  created_at        timestamp(6) with time zone NOT NULL DEFAULT now(),
  room_id           uuid                        NOT NULL,
  lecturer_ids      uuid[],
  student_group_ids uuid[],

  CONSTRAINT chk_bookings_time CHECK (start_time < end_time),
  CONSTRAINT fk_bookings_requested_room
    FOREIGN KEY (room_id) REFERENCES rooms (id)
);

CREATE INDEX idx_bookings_time_brin
  ON bookings USING brin (start_time, end_time);

CREATE INDEX idx_bookings_start_time ON bookings USING brin (start_time);
CREATE INDEX idx_bookings_end_time ON bookings USING brin (end_time);

CREATE TABLE booking_allocations
(
  booking_id uuid                        NOT NULL,
  room_id    uuid                        NOT NULL,
  start_time timestamp(6) with time zone NOT NULL,
  end_time   timestamp(6) with time zone NOT NULL,

  CONSTRAINT pk_booking_allocations PRIMARY KEY (booking_id, room_id),
  CONSTRAINT fk_alloc_on_booking
    FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
  CONSTRAINT fk_alloc_on_room
    FOREIGN KEY (room_id) REFERENCES rooms (id),
  CONSTRAINT chk_alloc_time CHECK (start_time < end_time)
);

ALTER TABLE booking_allocations
  ADD CONSTRAINT booking_allocations_no_overlap
    EXCLUDE USING gist (
    room_id WITH =,
    tstzrange(start_time, end_time, '[)') WITH &&
    );

CREATE INDEX idx_booking_allocations_room_start
  ON booking_allocations (room_id, start_time);
