create table simulation_seed (
    seed_key varchar(80) primary key,
    description varchar(180) not null,
    created_at timestamptz not null default current_timestamp
);

insert into simulation_seed(seed_key, description)
values ('KZ-RETAIL-DEMO-V1', 'Synthetic Kazakhstan retail accounts created through idempotent core commands');
