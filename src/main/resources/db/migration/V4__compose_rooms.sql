BEGIN;

ALTER TABLE rooms
  ADD COLUMN IF NOT EXISTS parent_room_id uuid;

ALTER TABLE rooms
  ADD CONSTRAINT fk_rooms_on_parent FOREIGN KEY (parent_room_id) REFERENCES rooms (id) ON DELETE SET NULL;

ALTER TABLE rooms
  ADD CONSTRAINT chk_rooms_no_self_parent CHECK (parent_room_id IS NULL OR parent_room_id <> id);

CREATE INDEX IF NOT EXISTS idx_rooms_parent_room_id ON rooms (parent_room_id);

CREATE TABLE booking_allocations
(
  booking_id uuid                        NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
  room_id    uuid                        NOT NULL REFERENCES rooms (id),
  start_time timestamp(6) with time zone NOT NULL,
  end_time   timestamp(6) with time zone NOT NULL,
  PRIMARY KEY (booking_id, room_id),
  CHECK (start_time < end_time)
);

ALTER TABLE booking_allocations
  ADD CONSTRAINT booking_allocations_no_overlap
    EXCLUDE USING gist (
    room_id WITH =,
    tstzrange(start_time, end_time, '[)') WITH &&
    );

CREATE INDEX IF NOT EXISTS idx_booking_allocations_room_start ON booking_allocations (room_id, start_time);

DO
$$
  DECLARE
    consname text;
  BEGIN
    SELECT conname
    INTO consname
    FROM pg_constraint
    WHERE conrelid = 'bookings'::regclass
      AND contype = 'x'
    LIMIT 1;

    IF consname IS NOT NULL THEN
      EXECUTE format('ALTER TABLE bookings DROP CONSTRAINT %I;', consname);
    END IF;
  END
$$ LANGUAGE plpgsql;

ALTER TABLE bookings
  DROP CONSTRAINT fk_bookings_on_rooms;
ALTER TABLE bookings
  DROP COLUMN room_id;

COMMIT;
