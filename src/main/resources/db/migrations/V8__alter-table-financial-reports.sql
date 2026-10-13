-- 1. Se antes eram colunas de texto ou nulas, podes recriá-las ou alterá-las para JSONB
ALTER TABLE financial_reports DROP COLUMN IF EXISTS industry_verticals;
ALTER TABLE financial_reports DROP COLUMN IF EXISTS geographic_revenue;

-- 2. Adicionar as novas colunas como JSONB para guardar as listas dinâmicas
ALTER TABLE financial_reports ADD COLUMN industry_verticals JSONB;
ALTER TABLE financial_reports ADD COLUMN geographic_revenue JSONB;