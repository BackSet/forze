-- Persistent RBAC: data-driven permissions, roles and role-permission mapping.
-- Canonical roles become seeded system roles (global, organization_id null);
-- organizations may additionally define custom roles. The ADMINISTRADOR system
-- role is marked all_permissions=true so it always grants every registered
-- permission (including future ones) without further migration.
--
-- Rollback / mitigation: this migration is additive. The three new tables can be
-- dropped and the previous CHECK constraint on budgeting_memberships.role can be
-- recreated to restore the V12 state. No membership data is modified (the role
-- column already stores canonical role codes as strings).

create table budgeting_permissions (
    id uuid primary key,
    code varchar(64) not null unique,
    area varchar(40) not null,
    description varchar(200) not null
);

create table budgeting_roles (
    id uuid primary key,
    organization_id uuid references budgeting_organizations(id) on delete cascade,
    code varchar(64) not null,
    name varchar(120) not null,
    is_system boolean not null default false,
    all_permissions boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint ck_budgeting_roles_code_not_blank check (length(trim(code)) > 0)
);

-- System roles are global and unique by code; custom roles are unique per organization.
create unique index uq_budgeting_roles_system_code on budgeting_roles (code) where organization_id is null;
create unique index uq_budgeting_roles_org_code on budgeting_roles (organization_id, code) where organization_id is not null;
create index ix_budgeting_roles_org on budgeting_roles (organization_id);

create table budgeting_role_permissions (
    role_id uuid not null references budgeting_roles(id) on delete cascade,
    permission_id uuid not null references budgeting_permissions(id) on delete cascade,
    primary key (role_id, permission_id)
);

-- Seed registered permissions (codes mirror ForzePermission). Idempotent.
insert into budgeting_permissions (id, code, area, description) values
    (gen_random_uuid(), 'PROYECTOS_READ',       'PROYECTOS',      'Ver proyectos'),
    (gen_random_uuid(), 'PROYECTOS_WRITE',      'PROYECTOS',      'Crear y editar proyectos'),
    (gen_random_uuid(), 'PRESUPUESTOS_READ',    'PRESUPUESTOS',   'Ver presupuestos y versiones'),
    (gen_random_uuid(), 'PRESUPUESTOS_WRITE',   'PRESUPUESTOS',   'Crear y editar presupuestos y versiones'),
    (gen_random_uuid(), 'CATALOGOS_READ',       'CATALOGOS',      'Ver catalogos tecnicos'),
    (gen_random_uuid(), 'CATALOGOS_WRITE',      'CATALOGOS',      'Crear y editar catalogos tecnicos'),
    (gen_random_uuid(), 'PROVEEDORES_READ',     'PROVEEDORES',    'Ver proveedores y cotizaciones'),
    (gen_random_uuid(), 'PROVEEDORES_WRITE',    'PROVEEDORES',    'Crear y editar proveedores y cotizaciones'),
    (gen_random_uuid(), 'APROBACIONES_READ',    'APROBACIONES',   'Ver flujo de aprobaciones'),
    (gen_random_uuid(), 'APROBACIONES_WRITE',   'APROBACIONES',   'Aprobar, observar y rechazar versiones'),
    (gen_random_uuid(), 'DOCUMENTOS_READ',      'DOCUMENTOS',     'Ver documentos'),
    (gen_random_uuid(), 'DOCUMENTOS_WRITE',     'DOCUMENTOS',     'Generar documentos'),
    (gen_random_uuid(), 'ADMINISTRACION_READ',  'ADMINISTRACION', 'Ver administracion de usuarios, roles y membresias'),
    (gen_random_uuid(), 'ADMINISTRACION_WRITE', 'ADMINISTRACION', 'Administrar usuarios, roles y membresias'),
    (gen_random_uuid(), 'AUDITORIA_READ',       'AUDITORIA',      'Ver registros de auditoria')
on conflict (code) do nothing;

-- Seed canonical system roles (global). Guarded for idempotency.
insert into budgeting_roles (id, organization_id, code, name, is_system, all_permissions, created_at, updated_at, version)
select gen_random_uuid(), null, 'ADMINISTRADOR', 'Administrador', true, true, now(), now(), 0
where not exists (select 1 from budgeting_roles where organization_id is null and code = 'ADMINISTRADOR');

insert into budgeting_roles (id, organization_id, code, name, is_system, all_permissions, created_at, updated_at, version)
select gen_random_uuid(), null, 'PRESUPUESTISTA', 'Presupuestista', true, false, now(), now(), 0
where not exists (select 1 from budgeting_roles where organization_id is null and code = 'PRESUPUESTISTA');

insert into budgeting_roles (id, organization_id, code, name, is_system, all_permissions, created_at, updated_at, version)
select gen_random_uuid(), null, 'APROBADOR', 'Aprobador', true, false, now(), now(), 0
where not exists (select 1 from budgeting_roles where organization_id is null and code = 'APROBADOR');

insert into budgeting_roles (id, organization_id, code, name, is_system, all_permissions, created_at, updated_at, version)
select gen_random_uuid(), null, 'COMPRAS', 'Compras', true, false, now(), now(), 0
where not exists (select 1 from budgeting_roles where organization_id is null and code = 'COMPRAS');

-- Seed role->permission mapping for non-admin canonical roles (ADMINISTRADOR uses all_permissions).
insert into budgeting_role_permissions (role_id, permission_id)
select r.id, p.id
from budgeting_roles r
join budgeting_permissions p on p.code in (
    'PROYECTOS_READ','PROYECTOS_WRITE','PRESUPUESTOS_READ','PRESUPUESTOS_WRITE',
    'CATALOGOS_READ','CATALOGOS_WRITE','PROVEEDORES_READ','APROBACIONES_READ',
    'DOCUMENTOS_READ','DOCUMENTOS_WRITE','AUDITORIA_READ'
)
where r.organization_id is null and r.code = 'PRESUPUESTISTA'
on conflict do nothing;

insert into budgeting_role_permissions (role_id, permission_id)
select r.id, p.id
from budgeting_roles r
join budgeting_permissions p on p.code in (
    'PROYECTOS_READ','PRESUPUESTOS_READ','CATALOGOS_READ','PROVEEDORES_READ',
    'APROBACIONES_READ','APROBACIONES_WRITE','DOCUMENTOS_READ','AUDITORIA_READ'
)
where r.organization_id is null and r.code = 'APROBADOR'
on conflict do nothing;

insert into budgeting_role_permissions (role_id, permission_id)
select r.id, p.id
from budgeting_roles r
join budgeting_permissions p on p.code in (
    'PROYECTOS_READ','PRESUPUESTOS_READ','CATALOGOS_READ','CATALOGOS_WRITE',
    'PROVEEDORES_READ','PROVEEDORES_WRITE','APROBACIONES_READ','DOCUMENTOS_READ'
)
where r.organization_id is null and r.code = 'COMPRAS'
on conflict do nothing;

-- Allow custom (non-canonical) role codes on memberships now that roles are data-driven.
alter table budgeting_memberships drop constraint if exists ck_budgeting_memberships_role;
