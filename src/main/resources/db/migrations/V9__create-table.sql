-- Tabela filha para as Verticais de Indústria
CREATE TABLE IF NOT EXISTS financial_report_verticals (
    id BIGSERIAL PRIMARY KEY,
    financial_report_id BIGINT NOT NULL,
    vertical_name VARCHAR(255) NOT NULL,
    revenue NUMERIC(15, 2),
    percentage NUMERIC(5, 2),
    CONSTRAINT fk_report_verticals FOREIGN KEY (financial_report_id)
        REFERENCES financial_reports(id) ON DELETE CASCADE
);

-- Tabela filha para a Distribuição Geográfica
CREATE TABLE IF NOT EXISTS financial_report_geography (
    id BIGSERIAL PRIMARY KEY,
    financial_report_id BIGINT NOT NULL,
    region_name VARCHAR(255) NOT NULL,
    revenue NUMERIC(15, 2),
    percentage NUMERIC(5, 2),
    CONSTRAINT fk_report_geography FOREIGN KEY (financial_report_id)
        REFERENCES financial_reports(id) ON DELETE CASCADE
);