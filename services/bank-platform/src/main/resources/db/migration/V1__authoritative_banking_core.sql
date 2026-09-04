create table ledger_account (
    id varchar(96) primary key,
    code varchar(64) not null unique,
    name varchar(160) not null,
    account_class varchar(16) not null check (account_class in ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    normal_side varchar(6) not null check (normal_side in ('DEBIT', 'CREDIT')),
    currency char(3) not null check (currency = 'KZT'),
    status varchar(16) not null check (status in ('PENDING_OPEN', 'ACTIVE', 'FROZEN', 'CLOSED')),
    system_key varchar(96) unique,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    check (
        (account_class in ('ASSET', 'EXPENSE') and normal_side = 'DEBIT') or
        (account_class in ('LIABILITY', 'EQUITY', 'REVENUE') and normal_side = 'CREDIT')
    )
);

create table customer_account (
    account_id varchar(96) primary key references ledger_account(id),
    customer_id uuid not null,
    account_number varchar(64) not null unique,
    display_name varchar(120) not null
);

create index customer_account_customer_idx on customer_account(customer_id);

create table account_balance (
    account_id varchar(96) primary key references ledger_account(id),
    book_balance numeric(19, 2) not null default 0.00 check (book_balance >= 0),
    version bigint not null default 0 check (version >= 0),
    as_of timestamptz not null
);

create table journal_transaction (
    id varchar(96) primary key,
    source_command_id uuid not null unique,
    booked_at timestamptz not null,
    description varchar(180) not null,
    reversal_of varchar(96) unique references journal_transaction(id),
    currency char(3) not null check (currency = 'KZT')
);

create index journal_transaction_booked_idx on journal_transaction(booked_at desc, id desc);

create table ledger_entry (
    id bigint generated always as identity primary key,
    journal_id varchar(96) not null references journal_transaction(id),
    line_number smallint not null check (line_number > 0),
    account_id varchar(96) not null references ledger_account(id),
    side varchar(6) not null check (side in ('DEBIT', 'CREDIT')),
    amount numeric(19, 2) not null check (amount > 0),
    narrative varchar(220) not null,
    unique (journal_id, line_number)
);

create index ledger_entry_account_idx on ledger_entry(account_id, journal_id);

create table account_hold (
    id varchar(96) primary key,
    account_id varchar(96) not null references customer_account(account_id),
    amount numeric(19, 2) not null check (amount > 0),
    currency char(3) not null check (currency = 'KZT'),
    status varchar(10) not null check (status in ('ACTIVE', 'CAPTURED', 'RELEASED', 'EXPIRED')),
    created_at timestamptz not null,
    expires_at timestamptz not null,
    resolved_at timestamptz,
    check (expires_at > created_at),
    check ((status = 'ACTIVE' and resolved_at is null) or (status <> 'ACTIVE' and resolved_at is not null))
);

create index account_hold_active_idx on account_hold(account_id, expires_at) where status = 'ACTIVE';

create table idempotency_record (
    actor_id varchar(128) not null,
    operation varchar(64) not null,
    idempotency_key varchar(128) not null,
    request_hash char(64) not null,
    response_json jsonb not null,
    created_at timestamptz not null,
    primary key (actor_id, operation, idempotency_key)
);

create table outbox_event (
    id uuid primary key,
    occurred_at timestamptz not null,
    aggregate_type varchar(40) not null,
    aggregate_id varchar(96) not null,
    event_type varchar(80) not null,
    payload jsonb not null,
    published_at timestamptz
);

create index outbox_unpublished_idx on outbox_event(occurred_at, id) where published_at is null;

create table audit_event (
    id uuid primary key,
    occurred_at timestamptz not null,
    actor_id varchar(128) not null,
    action varchar(80) not null,
    aggregate_type varchar(40) not null,
    aggregate_id varchar(96) not null,
    details jsonb not null
);

create index audit_event_aggregate_idx on audit_event(aggregate_type, aggregate_id, occurred_at);

create function reject_immutable_history_change() returns trigger
language plpgsql
as $$
begin
    raise exception '% is immutable; append a reversal or a new event instead', tg_table_name
        using errcode = '55000';
end;
$$;

create trigger journal_transaction_immutable
before update or delete on journal_transaction
for each row execute function reject_immutable_history_change();

create trigger ledger_entry_immutable
before update or delete on ledger_entry
for each row execute function reject_immutable_history_change();

create trigger audit_event_immutable
before update or delete on audit_event
for each row execute function reject_immutable_history_change();

create function enforce_balanced_journal() returns trigger
language plpgsql
as $$
declare
    debit_total numeric(19, 2);
    credit_total numeric(19, 2);
begin
    select coalesce(sum(amount) filter (where side = 'DEBIT'), 0.00),
           coalesce(sum(amount) filter (where side = 'CREDIT'), 0.00)
      into debit_total, credit_total
      from ledger_entry
     where journal_id = coalesce(new.journal_id, old.journal_id);
    if debit_total <> credit_total then
        raise exception 'journal % is not balanced: debit %, credit %',
            coalesce(new.journal_id, old.journal_id), debit_total, credit_total
            using errcode = '23514';
    end if;
    return null;
end;
$$;

create constraint trigger journal_must_balance
after insert on ledger_entry
deferrable initially deferred
for each row execute function enforce_balanced_journal();
