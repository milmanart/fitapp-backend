
INSERT INTO dish_templates(dish_key, name_en, name_pl, category, default_serving_g)
VALUES ('omelette', 'Omelette', 'Omlet', 'breakfast', 220)
ON CONFLICT (dish_key) DO NOTHING;

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'omelette', 'usda:171287', 'Egg, whole, raw, fresh', 'Jajko cale, surowe', 150, 0.6818, 1
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='omelette' AND food_key='usda:171287');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'omelette', 'usda:171265', 'Milk, whole', 'Mleko pelne', 30, 0.1364, 2
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='omelette' AND food_key='usda:171265');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'omelette', 'usda:173410', 'Butter, salted', 'Maslo solone', 15, 0.0682, 3
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='omelette' AND food_key='usda:173410');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'omelette', 'usda:173414', 'Cheese, cheddar', 'Ser cheddar', 25, 0.1136, 4
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='omelette' AND food_key='usda:173414');

INSERT INTO dish_templates(dish_key, name_en, name_pl, category, default_serving_g)
VALUES ('fried_eggs', 'Fried Eggs (Sunny Side Up)', 'Jajecznica (na oczko)', 'breakfast', 130)
ON CONFLICT (dish_key) DO NOTHING;

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'fried_eggs', 'usda:171287', 'Egg, whole, raw, fresh', 'Jajko cale, surowe', 110, 0.8462, 1
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='fried_eggs' AND food_key='usda:171287');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'fried_eggs', 'usda:173410', 'Butter, salted', 'Maslo solone', 15, 0.1154, 2
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='fried_eggs' AND food_key='usda:173410');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'fried_eggs', 'usda:173468', 'Salt, table', 'Sol', 2, 0.0154, 3
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='fried_eggs' AND food_key='usda:173468');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'fried_eggs', 'usda:170931', 'Spices, pepper, black', 'Pieprz czarny', 1, 0.0077, 4
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='fried_eggs' AND food_key='usda:170931');

INSERT INTO dish_templates(dish_key, name_en, name_pl, category, default_serving_g)
VALUES ('beef_steak', 'Beef Steak (Sirloin)', 'Stek wolowy', 'main', 250)
ON CONFLICT (dish_key) DO NOTHING;

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'beef_steak', 'usda:170237', 'Beef, loin, tenderloin steak, cooked, grilled', 'Poledwica wolowa, grillowana', 200, 0.80, 1
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='beef_steak' AND food_key='usda:170237');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'beef_steak', 'usda:171413', 'Oil, olive', 'Oliwa z oliwek', 15, 0.06, 2
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='beef_steak' AND food_key='usda:171413');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'beef_steak', 'usda:169230', 'Garlic, raw', 'Czosnek surowy', 5, 0.02, 3
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='beef_steak' AND food_key='usda:169230');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'beef_steak', 'usda:173410', 'Butter, salted', 'Maslo solone', 15, 0.06, 4
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='beef_steak' AND food_key='usda:173410');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'beef_steak', 'usda:173468', 'Salt, table', 'Sol', 3, 0.012, 5
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='beef_steak' AND food_key='usda:173468');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'beef_steak', 'usda:170931', 'Spices, pepper, black', 'Pieprz czarny', 2, 0.008, 6
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='beef_steak' AND food_key='usda:170931');

INSERT INTO dish_templates(dish_key, name_en, name_pl, category, default_serving_g)
VALUES ('pork_steak', 'Pork Steak (Loin Chop)', 'Stek wieprzowy (karkówka)', 'main', 250)
ON CONFLICT (dish_key) DO NOTHING;

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'pork_steak', 'usda:167831', 'Pork, loin, center rib chops, cooked, broiled', 'Wieprzowina, schab, grillowany', 200, 0.80, 1
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='pork_steak' AND food_key='usda:167831');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'pork_steak', 'usda:171413', 'Oil, olive', 'Oliwa z oliwek', 15, 0.06, 2
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='pork_steak' AND food_key='usda:171413');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'pork_steak', 'usda:169230', 'Garlic, raw', 'Czosnek surowy', 5, 0.02, 3
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='pork_steak' AND food_key='usda:169230');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'pork_steak', 'usda:173468', 'Salt, table', 'Sol', 3, 0.012, 4
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='pork_steak' AND food_key='usda:173468');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'pork_steak', 'usda:170931', 'Spices, pepper, black', 'Pieprz czarny', 2, 0.008, 5
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='pork_steak' AND food_key='usda:170931');

INSERT INTO dish_templates(dish_key, name_en, name_pl, category, default_serving_g)
VALUES ('borscht_ukrainian', 'Ukrainian Borscht', 'Barszcz ukrainski', 'soup', 400)
ON CONFLICT (dish_key) DO NOTHING;

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'borscht_ukrainian', 'usda:169145', 'Beets, raw', 'Buraki surowe', 100, 0.25, 1
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='borscht_ukrainian' AND food_key='usda:169145');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'borscht_ukrainian', 'usda:169975', 'Cabbage, raw', 'Kapusta biala surowa', 80, 0.20, 2
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='borscht_ukrainian' AND food_key='usda:169975');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'borscht_ukrainian', 'usda:170026', 'Potatoes, flesh and skin, raw', 'Ziemniaki surowe', 80, 0.20, 3
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='borscht_ukrainian' AND food_key='usda:170026');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'borscht_ukrainian', 'usda:170000', 'Onions, raw', 'Cebula surowa', 40, 0.10, 4
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='borscht_ukrainian' AND food_key='usda:170000');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'borscht_ukrainian', 'usda:170393', 'Carrots, raw', 'Marchewka surowa', 40, 0.10, 5
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='borscht_ukrainian' AND food_key='usda:170393');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'borscht_ukrainian', 'usda:170459', 'Tomato paste', 'Koncentrat pomidorowy', 20, 0.05, 6
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='borscht_ukrainian' AND food_key='usda:170459');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'borscht_ukrainian', 'usda:171257', 'Sour cream, cultured', 'Smietana', 30, 0.075, 7
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='borscht_ukrainian' AND food_key='usda:171257');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'borscht_ukrainian', 'usda:172233', 'Dill weed, fresh', 'Koper swiezy', 5, 0.0125, 8
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='borscht_ukrainian' AND food_key='usda:172233');

INSERT INTO dish_templates(dish_key, name_en, name_pl, category, default_serving_g)
VALUES ('zurek', 'Żurek (Polish Sour Rye Soup)', 'Żurek', 'soup', 400)
ON CONFLICT (dish_key) DO NOTHING;

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'zurek', 'usda:168885', 'Rye flour (sourdough starter)', 'Maka zytnia (zakwas)', 30, 0.075, 1
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='zurek' AND food_key='usda:168885');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'zurek', 'usda:171631', 'Sausage, Italian, pork (white sausage sub)', 'Kielbasa biala', 80, 0.20, 2
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='zurek' AND food_key='usda:171631');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'zurek', 'usda:170026', 'Potatoes, flesh and skin, raw', 'Ziemniaki surowe', 80, 0.20, 3
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='zurek' AND food_key='usda:170026');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'zurek', 'usda:171287', 'Egg, whole, raw, fresh (halved, boiled)', 'Jajko gotowane (polowka)', 50, 0.125, 4
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='zurek' AND food_key='usda:171287');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'zurek', 'usda:170000', 'Onions, raw', 'Cebula surowa', 30, 0.075, 5
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='zurek' AND food_key='usda:170000');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'zurek', 'usda:169230', 'Garlic, raw', 'Czosnek surowy', 5, 0.0125, 6
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='zurek' AND food_key='usda:169230');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'zurek', 'usda:170928', 'Marjoram, dried', 'Majeranek suszony', 3, 0.0075, 7
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='zurek' AND food_key='usda:170928');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'zurek', 'usda:173472', 'Horseradish, prepared', 'Chrzan tarty', 10, 0.025, 8
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='zurek' AND food_key='usda:173472');

INSERT INTO dish_templates(dish_key, name_en, name_pl, category, default_serving_g)
VALUES ('crepes', 'Crepes', 'Nalesniki', 'breakfast', 200)
ON CONFLICT (dish_key) DO NOTHING;

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'crepes', 'usda:168936', 'Wheat flour, all-purpose', 'Maka pszenna', 60, 0.30, 1
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='crepes' AND food_key='usda:168936');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'crepes', 'usda:171287', 'Egg, whole, raw, fresh', 'Jajko cale, surowe', 55, 0.275, 2
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='crepes' AND food_key='usda:171287');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'crepes', 'usda:171265', 'Milk, whole', 'Mleko pelne', 70, 0.35, 3
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='crepes' AND food_key='usda:171265');

INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order)
SELECT 'crepes', 'usda:173410', 'Butter, salted', 'Maslo solone', 15, 0.075, 4
WHERE NOT EXISTS (SELECT 1 FROM dish_template_ingredients WHERE dish_key='crepes' AND food_key='usda:173410');

INSERT INTO dish_dictionary(term, dish_key, lang) VALUES
  ('omelette', 'omelette', 'en'),
  ('omelet', 'omelette', 'en'),
  ('cheese omelette', 'omelette', 'en'),
  ('fried eggs', 'fried_eggs', 'en'),
  ('sunny side up', 'fried_eggs', 'en'),
  ('eggs sunny side up', 'fried_eggs', 'en'),
  ('beef steak', 'beef_steak', 'en'),
  ('steak beef', 'beef_steak', 'en'),
  ('sirloin steak', 'beef_steak', 'en'),
  ('tenderloin steak', 'beef_steak', 'en'),
  ('grilled steak', 'beef_steak', 'en'),
  ('pork steak', 'pork_steak', 'en'),
  ('pork chop', 'pork_steak', 'en'),
  ('pork loin', 'pork_steak', 'en'),
  ('grilled pork', 'pork_steak', 'en'),
  ('ukrainian borscht', 'borscht_ukrainian', 'en'),
  ('borscht', 'borscht_ukrainian', 'en'),
  ('beet soup', 'borscht_ukrainian', 'en'),
  ('borsch', 'borscht_ukrainian', 'en'),
  ('zurek', 'zurek', 'en'),
  ('sour rye soup', 'zurek', 'en'),
  ('polish sour soup', 'zurek', 'en'),
  ('crepes', 'crepes', 'en'),
  ('thin pancakes', 'crepes', 'en'),
  ('french crepes', 'crepes', 'en')
ON CONFLICT (lang, term) DO NOTHING;

INSERT INTO dish_dictionary(term, dish_key, lang) VALUES
  ('omlet', 'omelette', 'pl'),
  ('omlet z serem', 'omelette', 'pl'),
  ('jajecznica na oczko', 'fried_eggs', 'pl'),
  ('jajka sadzone', 'fried_eggs', 'pl'),
  ('jajko sadzone', 'fried_eggs', 'pl'),
  ('stek wolowy', 'beef_steak', 'pl'),
  ('stek z wolowiny', 'beef_steak', 'pl'),
  ('poledwica wolowa', 'beef_steak', 'pl'),
  ('befsztyk', 'beef_steak', 'pl'),
  ('stek wieprzowy', 'pork_steak', 'pl'),
  ('kotlet schabowy', 'pork_steak', 'pl'),
  ('schab', 'pork_steak', 'pl'),
  ('karkowka', 'pork_steak', 'pl'),
  ('barszcz ukrainski', 'borscht_ukrainian', 'pl'),
  ('barszcz', 'borscht_ukrainian', 'pl'),
  ('barszcz czerwony', 'borscht_ukrainian', 'pl'),
  ('zurek', 'zurek', 'pl'),
  ('zur', 'zurek', 'pl'),
  ('zurek z kielbasa', 'zurek', 'pl'),
  ('zurek z jajkiem', 'zurek', 'pl'),
  ('nalesniki', 'crepes', 'pl'),
  ('nalesnik', 'crepes', 'pl'),
  ('bliny', 'crepes', 'pl')
ON CONFLICT (lang, term) DO NOTHING;

UPDATE data_pack_versions
SET version = version + 1,
    updated_at = NOW()
WHERE pack_name IN ('dish_templates', 'lang_en', 'lang_pl');
