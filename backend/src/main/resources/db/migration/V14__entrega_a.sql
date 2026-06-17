-- Migration V14: Entrega A - Vigencia de precios y gestion de riesgos.

-- Alter budgeting_price_history to add price status, minimum order volume, and payment terms.
ALTER TABLE budgeting_price_history ADD COLUMN status varchar(20) NOT null DEFAULT 'VIGENTE';
ALTER TABLE budgeting_price_history ADD COLUMN min_order numeric(18, 4);
ALTER TABLE budgeting_price_history ADD COLUMN payment_terms varchar(200);

ALTER TABLE budgeting_price_history ADD CONSTRAINT ck_budgeting_price_history_status
    CHECK (status IN ('BORRADOR', 'VIGENTE', 'POR_VERIFICAR', 'VENCIDO', 'BLOQUEADO'));

ALTER TABLE budgeting_price_history ADD CONSTRAINT ck_budgeting_price_history_min_order
    CHECK (min_order IS null OR min_order >= 0);

-- Create budgeting_budget_risks table.
CREATE TABLE budgeting_budget_risks (
    id uuid PRIMARY KEY,
    organization_id uuid NOT null,
    budget_version_id uuid NOT null,
    description text NOT null,
    probability numeric(5, 4) NOT null,
    impact numeric(18, 2) NOT null,
    expected_amount numeric(18, 2) NOT null,
    assigned_to varchar(160),
    mitigation text,
    mitigated boolean NOT null DEFAULT false,
    created_at timestamptz NOT null,
    updated_at timestamptz NOT null,
    CONSTRAINT fk_budgeting_budget_risks_org FOREIGN KEY (organization_id)
        REFERENCES budgeting_organizations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_budgeting_budget_risks_version FOREIGN KEY (budget_version_id)
        REFERENCES budgeting_budget_versions (id) ON DELETE CASCADE,
    CONSTRAINT ck_budgeting_budget_risks_probability CHECK (probability >= 0 AND probability <= 1),
    CONSTRAINT ck_budgeting_budget_risks_impact CHECK (impact >= 0),
    CONSTRAINT ck_budgeting_budget_risks_expected CHECK (expected_amount >= 0)
);

CREATE INDEX ix_budgeting_budget_risks_org ON budgeting_budget_risks (organization_id);
CREATE INDEX ix_budgeting_budget_risks_version ON budgeting_budget_risks (budget_version_id);
