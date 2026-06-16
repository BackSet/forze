-- Budgeting module: scenarios (section 13). Each budget version may hold several scenarios.

create table budgeting_scenarios (
    id uuid primary key,
    budget_version_id uuid not null,
    name varchar(200) not null,
    type varchar(20) not null,
    is_primary boolean not null default false,
    utility_rate numeric(7, 4),
    indirect_rate numeric(7, 4),
    contingency_rate numeric(7, 4),
    duration_days integer,
    construction_method varchar(200),
    total_cost numeric(18, 2),
    sale_price numeric(18, 2),
    margin numeric(7, 4),
    risk varchar(10),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_scenarios_version foreign key (budget_version_id)
        references budgeting_budget_versions (id) on delete cascade,
    constraint ck_budgeting_scenarios_type check (type in
        ('BASE', 'ECONOMICO', 'RECOMENDADO', 'CONSERVADOR', 'PERSONALIZADO')),
    constraint ck_budgeting_scenarios_risk check (risk is null or risk in ('ALTO', 'MEDIO', 'BAJO')),
    constraint ck_budgeting_scenarios_rates check
        ((utility_rate is null or utility_rate >= 0)
         and (indirect_rate is null or indirect_rate >= 0)
         and (contingency_rate is null or contingency_rate >= 0))
);

create index ix_budgeting_scenarios_version on budgeting_scenarios (budget_version_id);

-- Section 13 variables configurables at component level (proveedor, precio, rendimiento, desperdicio).
create table budgeting_scenario_overrides (
    id uuid primary key,
    scenario_id uuid not null,
    item_apu_component_id uuid not null,
    supplier_id uuid,
    unit_price numeric(18, 4),
    yield numeric(18, 6),
    waste_factor numeric(18, 6),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_budgeting_scenario_ovr_scenario foreign key (scenario_id)
        references budgeting_scenarios (id) on delete cascade,
    constraint fk_budgeting_scenario_ovr_component foreign key (item_apu_component_id)
        references budgeting_item_apu_components (id) on delete cascade,
    constraint fk_budgeting_scenario_ovr_supplier foreign key (supplier_id)
        references budgeting_suppliers (id) on delete set null,
    constraint uq_budgeting_scenario_ovr unique (scenario_id, item_apu_component_id),
    constraint ck_budgeting_scenario_ovr_price check (unit_price is null or unit_price >= 0)
);

create index ix_budgeting_scenario_ovr_scenario on budgeting_scenario_overrides (scenario_id);
