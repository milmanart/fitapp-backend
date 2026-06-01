
INSERT INTO dish_templates(dish_key, name_en, name_pl, category, default_serving_g)
VALUES
  ('pizza_pepperoni', 'Pepperoni Pizza', 'Pizza pepperoni', 'pizza', 300),
  ('scrambled_eggs', 'Scrambled Eggs', 'Jajecznica', 'breakfast', 200),
  ('oatmeal', 'Oatmeal', 'Owsianka', 'breakfast', 260)
ON CONFLICT (dish_key) DO NOTHING;

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'pizza_pepperoni', 'usda:168936', 'All-purpose wheat flour', 'Maka pszenna (uniwersalna)', 150, 0.50, 1
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='pizza_pepperoni' AND food_key='usda:168936');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'pizza_pepperoni', 'usda:169074', 'Tomato sauce', 'Sos pomidorowy', 60, 0.20, 2
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='pizza_pepperoni' AND food_key='usda:169074');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'pizza_pepperoni', 'usda:170900', 'Mozzarella (shredded)', 'Mozzarella (tarta)', 60, 0.20, 3
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='pizza_pepperoni' AND food_key='usda:170900');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'pizza_pepperoni', 'usda:174575', 'Pepperoni (sliced)', 'Pepperoni (plastry)', 30, 0.10, 4
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='pizza_pepperoni' AND food_key='usda:174575');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'scrambled_eggs', 'usda:171287', 'Egg, whole, raw', 'Jajko cale, surowe', 150, 0.75, 1
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='scrambled_eggs' AND food_key='usda:171287');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'scrambled_eggs', 'usda:173410', 'Butter, salted', 'Maslo solone', 10, 0.05, 2
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='scrambled_eggs' AND food_key='usda:173410');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'scrambled_eggs', 'usda:171265', 'Milk, whole', 'Mleko pelne', 40, 0.20, 3
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='scrambled_eggs' AND food_key='usda:171265');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'oatmeal', 'usda:172989', 'Oats, quick, dry', 'Platki owsiane, blyskawiczne', 60, 0.2308, 1
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='oatmeal' AND food_key='usda:172989');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'oatmeal', 'usda:171265', 'Milk, whole', 'Mleko pelne', 200, 0.7692, 2
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='oatmeal' AND food_key='usda:171265');

INSERT INTO dish_dictionary(term, dish_key, lang)
VALUES
  ('pepperoni pizza', 'pizza_pepperoni', 'en'),
  ('pizza pepperoni', 'pizza_pepperoni', 'en'),
  ('scrambled eggs', 'scrambled_eggs', 'en'),
  ('oatmeal', 'oatmeal', 'en'),
  ('porridge', 'oatmeal', 'en')
ON CONFLICT (lang, term) DO NOTHING;

INSERT INTO dish_dictionary(term, dish_key, lang)
VALUES
  ('pizza pepperoni', 'pizza_pepperoni', 'pl'),
  ('pizza z pepperoni', 'pizza_pepperoni', 'pl'),
  ('jajecznica', 'scrambled_eggs', 'pl'),
  ('owsianka', 'oatmeal', 'pl')
ON CONFLICT (lang, term) DO NOTHING;

INSERT INTO data_pack_versions(pack_name, version) VALUES
  ('dish_templates', 0),
  ('lang_en', 0),
  ('lang_pl', 0),
  ('mini_usda', 0)
ON CONFLICT (pack_name) DO NOTHING;

UPDATE data_pack_versions
SET version = 1,
    updated_at = NOW()
WHERE pack_name IN ('dish_templates', 'lang_en', 'lang_pl', 'mini_usda');
