alter table rooms add column chem_symbol varchar(32) unique;

update rooms set chem_symbol = 'Ferrum' where chem_symbol is null;

alter table rooms alter column chem_symbol set not null;
