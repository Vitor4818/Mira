INSERT INTO tb_companies (name, ticker, sector, provider_type, api_url, currency, created_at)
VALUES
  (
    'CI&T',
    'CINT',
    'Tecnologia / Engenharia de Software',
    'MZIQ',
    'https://apicatalog.mziq.com/filemanager/company/81a95e6d-6ffb-4680-9289-fb47f63125e1/filter/categories/year/meta',
    'USD',
    NOW()
  ),
  (
    'TOTVS',
    'TOTS3',
    'Tecnologia / Software de Gestão (ERP)',
    'MZIQ',
    'https://apicatalog.mziq.com/filemanager/company/d3be5d49-62e7-4def-a3e1-ab25ff09f153/filter/categories/year/meta',
    'BRL',
    NOW()
  ),
  (
    'Endava',
    'DAVA',
    'Tecnologia / Engenharia de Software',
    'ENDAVA_SCRAPER',
    'https://investors.endava.com/financials/financial-results',
    'GBP',
    NOW()
  ),
  (
    'Accenture',
    'ACN',
    'Consultoria e Serviços de TI',
    'ACCENTURE',
    'https://investor.accenture.com',
    'USD',
    NOW()
  )
ON CONFLICT (ticker) DO UPDATE
SET
  name = EXCLUDED.name,
  sector = EXCLUDED.sector,
  provider_type = EXCLUDED.provider_type,
  api_url = EXCLUDED.api_url,
  currency = EXCLUDED.currency;