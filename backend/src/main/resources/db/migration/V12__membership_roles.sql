-- Budgeting module: memberships, roles and permissions (MVP 1).
create table budgeting_memberships (
    id uuid primary key,
    organization_id uuid not null references budgeting_organizations(id) on delete cascade,
    user_id uuid not null references identity_users(id) on delete cascade,
    role varchar(50) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint uq_budgeting_memberships_org_user unique (organization_id, user_id),
    constraint ck_budgeting_memberships_role check (role in ('ADMINISTRADOR', 'PRESUPUESTISTA', 'APROBADOR', 'COMPRAS'))
);

create index ix_budgeting_memberships_org on budgeting_memberships(organization_id);
create index ix_budgeting_memberships_user on budgeting_memberships(user_id);
