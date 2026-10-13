-- V6: Adiciona colunas para concentração de clientes e NRR (Net Retention Rate)
ALTER TABLE financial_reports
    ADD COLUMN IF NOT EXISTS top_client_concentration DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS top_5_clients_concentration DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS net_retention_rate DECIMAL(5,2);