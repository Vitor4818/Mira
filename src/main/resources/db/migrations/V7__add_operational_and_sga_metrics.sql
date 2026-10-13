-- V4: Adiciona métricas operacionais, de equipa e SG&A/Receita
ALTER TABLE financial_reports
    ADD COLUMN IF NOT EXISTS utilization_rate DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS attrition_rate DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS headcount INTEGER,
    ADD COLUMN IF NOT EXISTS revenue_per_employee DECIMAL(12,2),
    ADD COLUMN IF NOT EXISTS sga_to_revenue_ratio DECIMAL(5,2);