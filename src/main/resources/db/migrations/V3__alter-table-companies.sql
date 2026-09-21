-- Adiciona a coluna currency com fallback padrão BRL
ALTER TABLE tb_companies ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'BRL';

-- Atualiza as moedas reais das empresas já cadastradas
UPDATE tb_companies SET currency = 'BRL' WHERE ticker = 'TOTS3';
UPDATE tb_companies SET currency = 'USD' WHERE ticker IN ('CINT', 'ACN');
UPDATE tb_companies SET currency = 'GBP' WHERE ticker = 'DAVA';