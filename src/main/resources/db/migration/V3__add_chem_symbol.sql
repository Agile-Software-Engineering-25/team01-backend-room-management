alter table rooms
  add column chem_symbol varchar(32) unique not null;

alter table rooms
  add constraint unique_rooms_chem_symbol unique (chem_symbol);
