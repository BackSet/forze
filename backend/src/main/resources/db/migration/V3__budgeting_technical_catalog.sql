-- Budgeting module: technical catalog (sections 10.1, 10.2, 10.3, 11).
-- Insumos, master rubros, master APU and APU components.

-- Section 10.3 catalogo de insumos. Types fixed by the design enum.
create table budgeting_insumos (
    id uuid primary key,
    organization_id uuid not null,
    code varchar(60) not null,
    name varchar(200) not null,
    description text,
    unit_id uuid not null,
    type varchar(20) not null,
    category_id uuid,
    brand varchar(120),
    specification text,
    status varchar(20) not null default 'ACTIVO',
    reference_price numeric(18, 4),
    reference_price_currency varchar(3),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_insumos_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint fk_budgeting_insumos_unit foreign key (unit_id)
        references budgeting_units_of_measure (id) on delete restrict,
    constraint fk_budgeting_insumos_category foreign key (category_id)
        references budgeting_categories (id) on delete restrict,
    constraint uq_budgeting_insumos_org_code unique (organization_id, code),
    constraint ck_budgeting_insumos_type check (type in
        ('MATERIAL', 'MANO_DE_OBRA', 'EQUIPO', 'TRANSPORTE', 'SUBCONTRATO', 'OTRO')),
    constraint ck_budgeting_insumos_status check (status in ('ACTIVO', 'ARCHIVADO')),
    constraint ck_budgeting_insumos_price_non_negative check (reference_price is null or reference_price >= 0),
    constraint ck_budgeting_insumos_price_currency check
        (reference_price is null or reference_price_currency is not null)
);

create index ix_budgeting_insumos_org on budgeting_insumos (organization_id);
create index ix_budgeting_insumos_category on budgeting_insumos (category_id);

-- Section 10.2 catalogo de APU (master). APU keeps its own version number and lifecycle status.
create table budgeting_apu_maestros (
    id uuid primary key,
    organization_id uuid not null,
    code varchar(60) not null,
    name varchar(200) not null,
    unit_id uuid not null,
    category_id uuid,
    version_number integer not null default 1,
    status varchar(20) not null default 'BORRADOR',
    yield numeric(18, 6),
    estimated_cost numeric(18, 2),
    estimated_cost_currency varchar(3),
    valid_until date,
    author_user_id uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_apu_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint fk_budgeting_apu_unit foreign key (unit_id)
        references budgeting_units_of_measure (id) on delete restrict,
    constraint fk_budgeting_apu_category foreign key (category_id)
        references budgeting_categories (id) on delete restrict,
    constraint uq_budgeting_apu_org_code_version unique (organization_id, code, version_number),
    constraint ck_budgeting_apu_status check (status in
        ('BORRADOR', 'VALIDADO', 'VIGENTE', 'OBSOLETO', 'ARCHIVADO')),
    constraint ck_budgeting_apu_version_positive check (version_number > 0)
);

create index ix_budgeting_apu_org on budgeting_apu_maestros (organization_id);

-- Section 9.2 / 10.2 APU components. Section determines the component group.
create table budgeting_apu_components (
    id uuid primary key,
    apu_maestro_id uuid not null,
    section varchar(20) not null,
    insumo_id uuid,
    description varchar(200),
    unit_id uuid not null,
    quantity numeric(18, 4) not null,
    yield numeric(18, 6),
    unit_price numeric(18, 4) not null,
    waste_factor numeric(18, 6),
    position integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_budgeting_apu_comp_apu foreign key (apu_maestro_id)
        references budgeting_apu_maestros (id) on delete cascade,
    constraint fk_budgeting_apu_comp_insumo foreign key (insumo_id)
        references budgeting_insumos (id) on delete restrict,
    constraint fk_budgeting_apu_comp_unit foreign key (unit_id)
        references budgeting_units_of_measure (id) on delete restrict,
    constraint uq_budgeting_apu_comp_position unique (apu_maestro_id, position),
    constraint ck_budgeting_apu_comp_section check (section in
        ('MATERIALES', 'MANO_DE_OBRA', 'EQUIPOS', 'TRANSPORTE', 'SUBCONTRATOS', 'OTROS')),
    constraint ck_budgeting_apu_comp_quantity_non_negative check (quantity >= 0),
    constraint ck_budgeting_apu_comp_price_non_negative check (unit_price >= 0)
);

create index ix_budgeting_apu_comp_apu on budgeting_apu_components (apu_maestro_id);
create index ix_budgeting_apu_comp_insumo on budgeting_apu_components (insumo_id);

-- Section 10.1 catalogo de rubros (master). base_apu_id is the APU base of the rubro.
create table budgeting_rubros_maestros (
    id uuid primary key,
    organization_id uuid not null,
    code varchar(60) not null,
    name varchar(200) not null,
    description text,
    category_id uuid,
    unit_id uuid not null,
    specification text,
    keywords text,
    status varchar(20) not null default 'ACTIVO',
    base_apu_id uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_rubros_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint fk_budgeting_rubros_unit foreign key (unit_id)
        references budgeting_units_of_measure (id) on delete restrict,
    constraint fk_budgeting_rubros_category foreign key (category_id)
        references budgeting_categories (id) on delete restrict,
    constraint fk_budgeting_rubros_base_apu foreign key (base_apu_id)
        references budgeting_apu_maestros (id) on delete set null,
    constraint uq_budgeting_rubros_org_code unique (organization_id, code),
    constraint ck_budgeting_rubros_status check (status in ('ACTIVO', 'ARCHIVADO'))
);

create index ix_budgeting_rubros_org on budgeting_rubros_maestros (organization_id);
create index ix_budgeting_rubros_category on budgeting_rubros_maestros (category_id);

-- Section 10.1 "relacionar con rubros similares" / section 11 reuse suggestions.
create table budgeting_rubro_relations (
    rubro_id uuid not null,
    related_rubro_id uuid not null,
    created_at timestamptz not null,
    constraint pk_budgeting_rubro_relations primary key (rubro_id, related_rubro_id),
    constraint fk_budgeting_rubro_rel_rubro foreign key (rubro_id)
        references budgeting_rubros_maestros (id) on delete cascade,
    constraint fk_budgeting_rubro_rel_related foreign key (related_rubro_id)
        references budgeting_rubros_maestros (id) on delete cascade,
    constraint ck_budgeting_rubro_rel_not_self check (rubro_id <> related_rubro_id)
);
