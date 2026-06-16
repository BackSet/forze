-- Budgeting module: approval workflow (section 15). An approval request targets a budget version.

create table budgeting_approval_requests (
    id uuid primary key,
    budget_version_id uuid not null,
    status varchar(30) not null default 'PENDIENTE_APROBACION',
    submitted_by_user_id uuid,
    submitted_at timestamptz not null,
    decided_by_user_id uuid,
    decided_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint fk_budgeting_approvals_version foreign key (budget_version_id)
        references budgeting_budget_versions (id) on delete cascade,
    constraint ck_budgeting_approvals_status check (status in
        ('PENDIENTE_APROBACION', 'OBSERVADO', 'APROBADO', 'RECHAZADO')),
    constraint ck_budgeting_approvals_decided check
        ((decided_at is null) = (decided_by_user_id is null))
);

create index ix_budgeting_approvals_version on budgeting_approval_requests (budget_version_id);

-- Section 15 comentarios de aprobacion. rubro relacionado is an optional budget item.
create table budgeting_approval_comments (
    id uuid primary key,
    approval_request_id uuid not null,
    budget_item_id uuid,
    author_user_id uuid,
    comment text not null,
    response text,
    created_at timestamptz not null,
    constraint fk_budgeting_approval_comments_request foreign key (approval_request_id)
        references budgeting_approval_requests (id) on delete cascade,
    constraint fk_budgeting_approval_comments_item foreign key (budget_item_id)
        references budgeting_budget_items (id) on delete set null,
    constraint ck_budgeting_approval_comments_not_blank check (length(trim(comment)) > 0)
);

create index ix_budgeting_approval_comments_request on budgeting_approval_comments (approval_request_id);
