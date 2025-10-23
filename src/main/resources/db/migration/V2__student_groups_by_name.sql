alter table bookings
  alter column student_group_ids type varchar[] using student_group_ids::varchar[];
