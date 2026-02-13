CREATE TABLE dish_templates (
    dish_key VARCHAR(128) PRIMARY KEY,
    name_en VARCHAR(256) NOT NULL,
    name_pl VARCHAR(256),
    category VARCHAR(64),
    default_serving_g DECIMAL(10,2) NOT NULL DEFAULT 300,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE dish_template_ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dish_key VARCHAR(128) NOT NULL REFERENCES dish_templates(dish_key) ON DELETE CASCADE,

    food_key VARCHAR(128) NOT NULL,
    name_en VARCHAR(256) NOT NULL,
    name_pl VARCHAR(256),

    default_amount_g DECIMAL(10,2) NOT NULL,
    proportion_percent DECIMAL(6,4),
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_dish_template_ingredients_dish_key ON dish_template_ingredients(dish_key);
CREATE INDEX idx_dish_template_ingredients_food_key ON dish_template_ingredients(food_key);
