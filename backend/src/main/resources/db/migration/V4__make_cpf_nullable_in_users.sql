-- Make CPF nullable for establishments (they have CNPJ instead)
ALTER TABLE users ALTER COLUMN cpf DROP NOT NULL;