-- Budgeting module: suppliers, quotations and historical prices (sections 12.1, 12.2, 12.3, 9.4, 9.5).

-- Section 12.1 proveedores.
create table budgeting_suppliers (
    id uuid primary key,
    organization_id uuid not null,
    legal_name varchar(200) not null,
    tax_id varchar(40),
    contact_name varchar(160),
    phone varchar(60),
    email varchar(254),
    city varchar(120),
    offered_products text,
    payment_terms varchar(200),
    delivery_time varchar(120),
    rating numeric(3, 2),
    status varchar(20) not null default 'ACTIVO',
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_suppliers_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint uq_budgeting_suppliers_org_tax_id unique (organization_id, tax_id),
    constraint ck_budgeting_suppliers_status check (status in ('ACTIVO', 'INACTIVO')),
    constraint ck_budgeting_suppliers_rating check (rating is null or (rating >= 0 and rating <= 5)),
    constraint ck_budgeting_suppliers_name_not_blank check (length(trim(legal_name)) > 0)
);

create index ix_budgeting_suppliers_org on budgeting_suppliers (organization_id);

-- Section 12.2 cotizaciones.
create table budgeting_quotations (
    id uuid primary key,
    organization_id uuid not null,
    supplier_id uuid not null,
    quotation_date date not null,
    valid_until date,
    currency_code varchar(3) not null,
    tax_config_id uuid,
    transport_amount numeric(18, 4),
    conditions text,
    attachment_ref varchar(500),
    status varchar(20) not null default 'VIGENTE',
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_quotations_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint fk_budgeting_quotations_supplier foreign key (supplier_id)
        references budgeting_suppliers (id) on delete restrict,
    constraint fk_budgeting_quotations_tax foreign key (tax_config_id)
        references budgeting_tax_configs (id) on delete restrict,
    constraint ck_budgeting_quotations_status check (status in ('VIGENTE', 'EXPIRADA')),
    constraint ck_budgeting_quotations_valid_until check (valid_until is null or valid_until >= quotation_date)
);

create index ix_budgeting_quotations_org on budgeting_quotations (organization_id);
create index ix_budgeting_quotations_supplier on budgeting_quotations (supplier_id);

-- Section 12.2 productos cotizados / 12.3 comparador inputs.
create table budgeting_quotation_items (
    id uuid primary key,
    quotation_id uuid not null,
    insumo_id uuid,
    description varchar(200),
    unit_id uuid not null,
    unit_price numeric(18, 4) not null,
    min_order numeric(18, 4),
    discount numeric(7, 4),
    position integer not null,
    created_at timestamptz not null,
    constraint fk_budgeting_quotation_items_quotation foreign key (quotation_id)
        references budgeting_quotations (id) on delete cascade,
    constraint fk_budgeting_quotation_items_insumo foreign key (insumo_id)
        references budgeting_insumos (id) on delete restrict,
    constraint fk_budgeting_quotation_items_unit foreign key (unit_id)
        references budgeting_units_of_measure (id) on delete restrict,
    constraint uq_budgeting_quotation_items_position unique (quotation_id, position),
    constraint ck_budgeting_quotation_items_price_non_negative check (unit_price >= 0)
);

create index ix_budgeting_quotation_items_quotation on budgeting_quotation_items (quotation_id);
create index ix_budgeting_quotation_items_insumo on budgeting_quotation_items (insumo_id);

-- Sections 9.4 / 9.5 / 11 historical prices per insumo. Append-only history.
create table budgeting_price_history (
    id uuid primary key,
    organization_id uuid not null,
    insumo_id uuid not null,
    supplier_id uuid,
    quotation_id uuid,
    city varchar(120),
    price numeric(18, 4) not null,
    currency_code varchar(3) not null,
    price_date date not null,
    valid_until date,
    taxes_included boolean not null default false,
    transport_included boolean not null default false,
    created_at timestamptz not null,
    constraint fk_budgeting_price_history_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint fk_budgeting_price_history_insumo foreign key (insumo_id)
        references budgeting_insumos (id) on delete cascade,
    constraint fk_budgeting_price_history_supplier foreign key (supplier_id)
        references budgeting_suppliers (id) on delete set null,
    constraint fk_budgeting_price_history_quotation foreign key (quotation_id)
        references budgeting_quotations (id) on delete set null,
    constraint ck_budgeting_price_history_price_non_negative check (price >= 0),
    constraint ck_budgeting_price_history_valid_until check (valid_until is null or valid_until >= price_date)
);

create index ix_budgeting_price_history_insumo_date on budgeting_price_history (insumo_id, price_date);
create index ix_budgeting_price_history_org on budgeting_price_history (organization_id);
