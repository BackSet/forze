-- Budgeting module: clients and projects (sections 6.1, 6.2, 6.3).

create table budgeting_clients (
    id uuid primary key,
    organization_id uuid not null,
    name varchar(200) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_clients_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint uq_budgeting_clients_org_name unique (organization_id, name),
    constraint ck_budgeting_clients_name_not_blank check (length(trim(name)) > 0)
);

create index ix_budgeting_clients_org on budgeting_clients (organization_id);

-- Section 6.2 creacion de proyecto. current_budget_id references the live/approved budget
-- as a plain uuid (no FK) to avoid a projects<->budgets cycle.
create table budgeting_projects (
    id uuid primary key,
    organization_id uuid not null,
    code varchar(60) not null,
    name varchar(200) not null,
    client_id uuid,
    description text,
    work_type varchar(120),
    location varchar(200),
    estimated_start_date date,
    estimated_end_date date,
    currency_code varchar(3) not null,
    target_amount numeric(18, 2),
    minimum_margin numeric(7, 4),
    responsible_user_id uuid,
    current_budget_id uuid,
    status varchar(20) not null default 'ACTIVO',
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_projects_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint fk_budgeting_projects_client foreign key (client_id)
        references budgeting_clients (id) on delete restrict,
    constraint uq_budgeting_projects_org_code unique (organization_id, code),
    constraint ck_budgeting_projects_status check (status in ('ACTIVO', 'ARCHIVADO')),
    constraint ck_budgeting_projects_target_non_negative check (target_amount is null or target_amount >= 0),
    constraint ck_budgeting_projects_dates check
        (estimated_start_date is null or estimated_end_date is null or estimated_end_date >= estimated_start_date)
);

create index ix_budgeting_projects_org on budgeting_projects (organization_id);
create index ix_budgeting_projects_client on budgeting_projects (client_id);

-- Section 6.2 "Equipo del proyecto" / 6.3 Equipo tab. Members referenced by user id (no cross-module FK).
create table budgeting_project_team (
    project_id uuid not null,
    user_id uuid not null,
    created_at timestamptz not null,
    constraint pk_budgeting_project_team primary key (project_id, user_id),
    constraint fk_budgeting_project_team_project foreign key (project_id)
        references budgeting_projects (id) on delete cascade
);
