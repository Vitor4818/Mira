-- 1. Tabela Pai (Metadados do Relatório)
CREATE TABLE IF NOT EXISTS financial_reports (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 company VARCHAR(100),
    ticker VARCHAR(20),
    year INT,
    quarter INT
    );

-- 2. Tabela de Receita
CREATE TABLE IF NOT EXISTS revenues (
                                        id BIGSERIAL PRIMARY KEY,
                                        revenue NUMERIC(15,2),
    revenue_growth NUMERIC(5,2),
    financial_report_id BIGINT UNIQUE REFERENCES financial_reports(id) ON DELETE CASCADE
    );

-- 3. Tabela de Verticais de Indústria (Gestão, Techfin, RD Station)
CREATE TABLE IF NOT EXISTS industry_verticals (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  name VARCHAR(100),
    revenue NUMERIC(15,2),
    growth NUMERIC(5,2),
    revenue_id BIGINT REFERENCES revenues(id) ON DELETE CASCADE
    );

-- 4. Tabela de Clientes
CREATE TABLE IF NOT EXISTS clients (
                                       id BIGSERIAL PRIMARY KEY,
                                       top_clients_concentration NUMERIC(5,2),
    net_retention_rate NUMERIC(5,2),
    financial_report_id BIGINT UNIQUE REFERENCES financial_reports(id) ON DELETE CASCADE
    );

-- 5. Tabela Operacional
CREATE TABLE IF NOT EXISTS operational (
                                           id BIGSERIAL PRIMARY KEY,
                                           utilization_rate NUMERIC(5,2),
    attrition NUMERIC(5,2),
    employees INT,
    revenue_per_professional NUMERIC(15,2),
    financial_report_id BIGINT UNIQUE REFERENCES financial_reports(id) ON DELETE CASCADE
    );

-- 6. Tabela de Rentabilidade
CREATE TABLE IF NOT EXISTS profitability (
                                             id BIGSERIAL PRIMARY KEY,
                                             gross_margin NUMERIC(5,2),
    ebitda_margin NUMERIC(5,2),
    sga_to_revenue NUMERIC(5,2),
    financial_report_id BIGINT UNIQUE REFERENCES financial_reports(id) ON DELETE CASCADE
    );

-- 7. Tabela de Valuation
CREATE TABLE IF NOT EXISTS valuations (
                                          id BIGSERIAL PRIMARY KEY,
                                          free_cash_flow NUMERIC(15,2),
    financial_report_id BIGINT UNIQUE REFERENCES financial_reports(id) ON DELETE CASCADE
    );