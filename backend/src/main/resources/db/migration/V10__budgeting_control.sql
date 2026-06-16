-- Budgeting module: site control (section 18). The approved version becomes the baseline.
-- Projection (18.6) is a live computed report and is intentionally NOT persisted.

create table budgeting_control_baselines (
    id uuid primary key,
    project_id uuid not null,
    budget_version_id uuid not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_baselines_project foreign key (project_id)
        references budgeting_projects (id) on delete cascade,
    constraint fk_budgeting_baselines_version foreign key (budget_version_id)
        references budgeting_budget_versions (id) on delete restrict,
    constraint uq_budgeting_baselines_project unique (project_id)
);

-- Section 18.2 physical progress per item.
create table budgeting_progress_entries (
    id uuid primary key,
    baseline_id uuid not null,
    budget_item_id uuid not null,
    budgeted_quantity numeric(18, 4) not null,
    executed_quantity numeric(18, 4) not null default 0,
    entry_date date not null,
    responsible_user_id uuid,
    evidence_ref varchar(500),
    created_at timestamptz not null,
    constraint fk_budgeting_progress_baseline foreign key (baseline_id)
        references budgeting_control_baselines (id) on delete cascade,
    constraint fk_budgeting_progress_item foreign key (budget_item_id)
        references budgeting_budget_items (id) on delete restrict,
    constraint ck_budgeting_progress_qty_non_negative check
        (budgeted_quantity >= 0 and executed_quantity >= 0)
);

create index ix_budgeting_progress_baseline on budgeting_progress_entries (baseline_id);

-- Section 18.3 real costs.
create table budgeting_real_costs (
    id uuid primary key,
    baseline_id uuid not null,
    budget_item_id uuid,
    cost_type varchar(20) not null,
    amount numeric(18, 2) not null,
    currency_code varchar(3) not null,
    cost_date date not null,
    description varchar(300),
    created_at timestamptz not null,
    constraint fk_budgeting_real_costs_baseline foreign key (baseline_id)
        references budgeting_control_baselines (id) on delete cascade,
    constraint fk_budgeting_real_costs_item foreign key (budget_item_id)
        references budgeting_budget_items (id) on delete set null,
    constraint ck_budgeting_real_costs_type check (cost_type in
        ('COMPRA', 'MANO_DE_OBRA', 'EQUIPO', 'SUBCONTRATO', 'INDIRECTO', 'ADICIONAL')),
    constraint ck_budgeting_real_costs_amount_non_negative check (amount >= 0)
);

create index ix_budgeting_real_costs_baseline on budgeting_real_costs (baseline_id);

-- Section 18.5 additionals.
create table budgeting_additionals (
    id uuid primary key,
    baseline_id uuid not null,
    budget_item_id uuid,
    description text not null,
    quantity numeric(18, 4),
    amount numeric(18, 2),
    amount_currency varchar(3),
    status varchar(20) not null default 'PROPUESTO',
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_additionals_baseline foreign key (baseline_id)
        references budgeting_control_baselines (id) on delete cascade,
    constraint fk_budgeting_additionals_item foreign key (budget_item_id)
        references budgeting_budget_items (id) on delete set null,
    constraint ck_budgeting_additionals_status check (status in
        ('PROPUESTO', 'EN_REVISION', 'APROBADO', 'RECHAZADO', 'EJECUTADO', 'FACTURADO')),
    constraint ck_budgeting_additionals_amount_non_negative check (amount is null or amount >= 0)
);

create index ix_budgeting_additionals_baseline on budgeting_additionals (baseline_id);
