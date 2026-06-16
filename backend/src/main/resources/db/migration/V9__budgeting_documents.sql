-- Budgeting module: client documents generated from a budget version (section 17).

create table budgeting_documents (
    id uuid primary key,
    organization_id uuid not null,
    budget_version_id uuid not null,
    type varchar(30) not null,
    format varchar(10) not null,
    number varchar(60),
    valid_until date,
    notes text,
    generated_by_user_id uuid,
    created_at timestamptz not null,
    constraint fk_budgeting_documents_org foreign key (organization_id)
        references budgeting_organizations (id) on delete restrict,
    constraint fk_budgeting_documents_version foreign key (budget_version_id)
        references budgeting_budget_versions (id) on delete cascade,
    constraint ck_budgeting_documents_type check (type in
        ('COTIZACION', 'PRESUPUESTO_DETALLADO', 'RESUMEN_CAPITULOS',
         'TERMINOS_CONDICIONES', 'CRONOGRAMA_VALORIZADO', 'PROPUESTA_ECONOMICA')),
    constraint ck_budgeting_documents_format check (format in ('PDF', 'EXCEL', 'CSV'))
);

create index ix_budgeting_documents_version on budgeting_documents (budget_version_id);
create index ix_budgeting_documents_org on budgeting_documents (organization_id);
