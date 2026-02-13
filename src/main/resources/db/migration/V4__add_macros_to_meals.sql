-- Add macronutrients to meals table
ALTER TABLE meals ADD COLUMN proteins DECIMAL(10, 2) DEFAULT 0;
ALTER TABLE meals ADD COLUMN fats DECIMAL(10, 2) DEFAULT 0;
ALTER TABLE meals ADD COLUMN carbohydrates DECIMAL(10, 2) DEFAULT 0;

-- Update calories column to support decimals for more precision
ALTER TABLE meals ALTER COLUMN calories TYPE DECIMAL(10, 2);
