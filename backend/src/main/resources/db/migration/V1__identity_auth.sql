create table identity_users (
    id uuid primary key,
    username varchar(80) not null,
    email varchar(254),
    password_hash varchar(255) not null,
    enabled boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uq_identity_users_username unique (username),
    constraint uq_identity_users_email unique (email),
    constraint ck_identity_users_username_not_blank check (length(trim(username)) > 0),
    constraint ck_identity_users_email_not_blank check (email is null or length(trim(email)) > 0)
);

create table identity_refresh_tokens (
    id uuid primary key,
    user_id uuid not null references identity_users(id) on delete cascade,
    token_hash varchar(128) not null,
    family_id uuid not null,
    issued_at timestamptz not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    replaced_by_token_id uuid,
    reuse_detected_at timestamptz,
    constraint uq_identity_refresh_tokens_token_hash unique (token_hash),
    constraint fk_identity_refresh_replacement foreign key (replaced_by_token_id) references identity_refresh_tokens(id),
    constraint ck_identity_refresh_expiration check (expires_at > issued_at)
);

create index ix_identity_refresh_tokens_user_id on identity_refresh_tokens(user_id);
create index ix_identity_refresh_tokens_family_id on identity_refresh_tokens(family_id);
create index ix_identity_refresh_tokens_expires_at on identity_refresh_tokens(expires_at);
