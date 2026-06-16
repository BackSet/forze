-- Budgeting module: budgets, versions and the item tree (sections 7, 8, 9, 16).
-- A budget belongs to a project and holds one or more versions. Each version owns an
-- independent snapshot of the chapter/item/APU tree so an approved version stays reproducible
-- even when the technical catalog or prices change later (section 16).

create table budgeting_budgets (
    id uuid primary key,
    organization_id uuid not null,
    project_id uuid not null,
    code varchar(60) not null,
    name varchar(200) not null,
    currency_code varchar(3) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_budgets_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint fk_budgeting_budgets_project foreign key (project_id)
        references budgeting_projects (id) on delete restrict,
    constraint uq_budgeting_budgets_project_code unique (project_id, code)
);

create index ix_budgeting_budgets_project on budgeting_budgets (project_id);

-- Section 7.1 states + section 16 versioning. Stored financial totals are frozen snapshots
-- (computed on calculation/approval) kept for historical comparison between versions.
create table budgeting_budget_versions (
    id uuid primary key,
    budget_id uuid not null,
    version_number integer not null,
    name varchar(200),
    description text,
    status varchar(30) not null default 'BORRADOR',
    change_reason text,
    created_by_user_id uuid,
    target_amount numeric(18, 2),
    utility_rate numeric(7, 4),
    indirect_rate numeric(7, 4),
    contingency_rate numeric(7, 4),
    tax_config_id uuid,
    valid_until date,
    viability_status varchar(20),
    total_cost numeric(18, 2),
    sale_price numeric(18, 2),
    margin numeric(7, 4),
    approved_at timestamptz,
    approved_by_user_id uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_versions_budget foreign key (budget_id)
        references budgeting_budgets (id) on delete cascade,
    constraint fk_budgeting_versions_tax foreign key (tax_config_id)
        references budgeting_tax_configs (id) on delete restrict,
    constraint uq_budgeting_versions_budget_number unique (budget_id, version_number),
    constraint ck_budgeting_versions_number_positive check (version_number > 0),
    constraint ck_budgeting_versions_status check (status in
        ('BORRADOR', 'EN_CALCULO', 'REQUIERE_AJUSTES', 'PENDIENTE_APROBACION',
         'APROBADO', 'ENVIADO', 'ACEPTADO', 'RECHAZADO', 'ARCHIVADO')),
    constraint ck_budgeting_versions_viability check
        (viability_status is null or viability_status in ('VIABLE', 'VIABLE_CON_ALERTAS', 'NO_VIABLE')),
    constraint ck_budgeting_versions_rates check
        ((utility_rate is null or utility_rate >= 0)
         and (indirect_rate is null or indirect_rate >= 0)
         and (contingency_rate is null or contingency_rate >= 0))
);

create index ix_budgeting_versions_budget on budgeting_budget_versions (budget_id);

-- Section 8.4 chapters / subchapters. parent_chapter_id models subcapitulos.
create table budgeting_chapters (
    id uuid primary key,
    budget_version_id uuid not null,
    parent_chapter_id uuid,
    code varchar(60),
    name varchar(200) not null,
    position integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_budgeting_chapters_version foreign key (budget_version_id)
        references budgeting_budget_versions (id) on delete cascade,
    constraint fk_budgeting_chapters_parent foreign key (parent_chapter_id)
        references budgeting_chapters (id) on delete cascade,
    constraint ck_budgeting_chapters_name_not_blank check (length(trim(name)) > 0)
);

create index ix_budgeting_chapters_version on budgeting_chapters (budget_version_id);
create index ix_budgeting_chapters_parent on budgeting_chapters (parent_chapter_id);

-- Section 8.4 / 9.1 budget item (rubro snapshot inside a version).
-- source_rubro_id keeps the catalog link (set null on catalog delete); the snapshot fields survive.
create table budgeting_budget_items (
    id uuid primary key,
    budget_version_id uuid not null,
    chapter_id uuid,
    source_rubro_id uuid,
    code varchar(60),
    name varchar(200) not null,
    description text,
    unit_id uuid not null,
    quantity numeric(18, 4) not null default 0,
    unit_cost numeric(18, 4),
    unit_price numeric(18, 4),
    price_locked boolean not null default false,
    total_cost numeric(18, 2),
    total_sale numeric(18, 2),
    margin numeric(7, 4),
    category_id uuid,
    valid_until date,
    validation_status varchar(20),
    notes text,
    position integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_budgeting_items_version foreign key (budget_version_id)
        references budgeting_budget_versions (id) on delete cascade,
    constraint fk_budgeting_items_chapter foreign key (chapter_id)
        references budgeting_chapters (id) on delete set null,
    constraint fk_budgeting_items_rubro foreign key (source_rubro_id)
        references budgeting_rubros_maestros (id) on delete set null,
    constraint fk_budgeting_items_unit foreign key (unit_id)
        references budgeting_units_of_measure (id) on delete restrict,
    constraint fk_budgeting_items_category foreign key (category_id)
        references budgeting_categories (id) on delete restrict,
    constraint ck_budgeting_items_quantity_non_negative check (quantity >= 0),
    constraint ck_budgeting_items_validation check
        (validation_status is null or validation_status in ('COMPLETO', 'INCOMPLETO'))
);

create index ix_budgeting_items_version on budgeting_budget_items (budget_version_id);
create index ix_budgeting_items_chapter on budgeting_budget_items (chapter_id);
create index ix_budgeting_items_rubro on budgeting_budget_items (source_rubro_id);

-- Section 9.2 APU snapshot for the item (one APU per item).
create table budgeting_item_apu (
    id uuid primary key,
    budget_item_id uuid not null,
    source_apu_id uuid,
    yield numeric(18, 6),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_budgeting_item_apu_item foreign key (budget_item_id)
        references budgeting_budget_items (id) on delete cascade,
    constraint fk_budgeting_item_apu_source foreign key (source_apu_id)
        references budgeting_apu_maestros (id) on delete set null,
    constraint uq_budgeting_item_apu_item unique (budget_item_id)
);

-- Section 9.2 APU component snapshot. unit_price is FROZEN here: this is the historical
-- price used in the version and must not change when the catalog/price history changes.
create table budgeting_item_apu_components (
    id uuid primary key,
    item_apu_id uuid not null,
    section varchar(20) not null,
    source_insumo_id uuid,
    description varchar(200),
    unit_id uuid not null,
    quantity numeric(18, 4) not null,
    yield numeric(18, 6),
    unit_price numeric(18, 4) not null,
    waste_factor numeric(18, 6),
    price_locked boolean not null default false,
    price_source varchar(200),
    line_total numeric(18, 2),
    position integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_budgeting_item_comp_apu foreign key (item_apu_id)
        references budgeting_item_apu (id) on delete cascade,
    constraint fk_budgeting_item_comp_insumo foreign key (source_insumo_id)
        references budgeting_insumos (id) on delete set null,
    constraint fk_budgeting_item_comp_unit foreign key (unit_id)
        references budgeting_units_of_measure (id) on delete restrict,
    constraint uq_budgeting_item_comp_position unique (item_apu_id, position),
    constraint ck_budgeting_item_comp_section check (section in
        ('MATERIALES', 'MANO_DE_OBRA', 'EQUIPOS', 'TRANSPORTE', 'SUBCONTRATOS', 'OTROS')),
    constraint ck_budgeting_item_comp_quantity_non_negative check (quantity >= 0),
    constraint ck_budgeting_item_comp_price_non_negative check (unit_price >= 0)
);

create index ix_budgeting_item_comp_apu on budgeting_item_apu_components (item_apu_id);

-- Section 9.3 mediciones (quantity formulas per item).
create table budgeting_measurements (
    id uuid primary key,
    budget_item_id uuid not null,
    description varchar(200),
    length numeric(18, 4),
    width numeric(18, 4),
    height numeric(18, 4),
    item_count numeric(18, 4),
    factor numeric(18, 6),
    formula varchar(500),
    result numeric(18, 4),
    notes varchar(500),
    position integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_budgeting_measurements_item foreign key (budget_item_id)
        references budgeting_budget_items (id) on delete cascade,
    constraint uq_budgeting_measurements_position unique (budget_item_id, position)
);

create index ix_budgeting_measurements_item on budgeting_measurements (budget_item_id);
