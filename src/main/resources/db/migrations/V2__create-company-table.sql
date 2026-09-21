CREATE TABLE IF NOT EXISTS "TB_companies" (
                                              id BIGSERIAL PRIMARY KEY,
                                              name VARCHAR(255) NOT NULL UNIQUE,
    ticker VARCHAR(20) NOT NULL UNIQUE,
    sector VARCHAR(255) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    api_url TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );

INSERT INTO "TB_companies" (name, ticker, sector, provider_type, api_url, created_at)
VALUES (
           'TOTVS',
           'TOTS3',
           'Tecnologia / Software de Gestão',
           'MZIQ',
           'https://api.mziq.com/mz/services/portal/reports/v1/documents',
           CURRENT_TIMESTAMP
       )
    ON CONFLICT (ticker) DO NOTHING;