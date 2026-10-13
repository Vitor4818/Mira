ALTER TABLE financial_reports
    ADD COLUMN IF NOT EXISTS vertical_exposure TEXT,
    ADD COLUMN IF NOT EXISTS geographic_revenue TEXT;