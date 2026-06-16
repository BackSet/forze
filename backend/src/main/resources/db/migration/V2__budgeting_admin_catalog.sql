-- Budgeting module: organization tenant root and administration catalogs.
-- Source design: sections 2.1 (admin configures organization, taxes, units, categories) and 4 (organization selector).
-- Numeric strategy (confirmed): money unit numeric(18,4), money total numeric(18,2),
-- quantity numeric(18,4), yield/waste numeric(18,6), percentage numeric(7,4), currency varchar(3) ISO-4217.

create table budgeting_organizations (
    id uuid primary key,
    name varchar(160) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint ck_budgeting_organizations_name_not_blank check (length(trim(name)) > 0)
);

create table budgeting_units_of_measure (
    id uuid primary key,
    organization_id uuid not null,
    code varchar(40) not null,
    name varchar(120) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_units_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint uq_budgeting_units_org_code unique (organization_id, code),
    constraint ck_budgeting_units_code_not_blank check (length(trim(code)) > 0)
);

create index ix_budgeting_units_org on budgeting_units_of_measure (organization_id);

create table budgeting_categories (
    id uuid primary key,
    organization_id uuid not null,
    code varchar(40) not null,
    name varchar(160) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_categories_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint uq_budgeting_categories_org_code unique (organization_id, code),
    constraint ck_budgeting_categories_code_not_blank check (length(trim(code)) > 0)
);

create index ix_budgeting_categories_org on budgeting_categories (organization_id);

-- Configurable taxes (section 2.1 "configurar impuestos"). Rate is a percentage.
create table budgeting_tax_configs (
    id uuid primary key,
    organization_id uuid not null,
    code varchar(40) not null,
    name varchar(120) not null,
    rate numeric(7, 4) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_tax_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint uq_budgeting_tax_org_code unique (organization_id, code),
    constraint ck_budgeting_tax_rate_non_negative check (rate >= 0)
);

create index ix_budgeting_tax_org on budgeting_tax_configs (organization_id);
