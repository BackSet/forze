-- Budgeting module: audit log (section 21). Append-only record of changes.

create table budgeting_audit_log (
    id uuid primary key,
    organization_id uuid,
    user_id uuid,
    action varchar(120) not null,
    entity_type varchar(120) not null,
    entity_id uuid,
    old_value text,
    new_value text,
    reason text,
    ip_address varchar(64),
    occurred_at timestamptz not null,
    constraint fk_budgeting_audit_org foreign key (organization_id)
        references budgeting_organizations (id) on delete set null,
    constraint ck_budgeting_audit_action_not_blank check (length(trim(action)) > 0),
    constraint ck_budgeting_audit_entity_not_blank check (length(trim(entity_type)) > 0)
);

create index ix_budgeting_audit_entity on budgeting_audit_log (entity_type, entity_id);
create index ix_budgeting_audit_occurred_at on budgeting_audit_log (occurred_at);
