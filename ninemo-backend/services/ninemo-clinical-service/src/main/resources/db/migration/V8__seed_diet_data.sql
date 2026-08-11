-- Seed diet_food_safety with common Indian ingredients
INSERT INTO diet_food_safety (ingredient_name, ingredient_name_hindi, safety_rating, medical_reasoning, safe_quantity, trimester_tags, categories) VALUES
('Papaya (raw/unripe)', 'Kaccha Papita', 'AVOID', 'Contains papain and latex which may cause uterine contractions and miscarriage', NULL, '[1,2,3]', '["FRUIT"]'),
('Papaya (ripe)', 'Pakka Papita', 'CAUTION', 'Ripe papaya in small amounts is generally safe; contains beneficial folate and vitamins', 'Small amounts (100g) occasionally', '[2,3]', '["FRUIT"]'),
('Pineapple', 'Ananas', 'CAUTION', 'Contains bromelain which may soften the cervix; small amounts generally safe', 'Small amounts only', '[1,2,3]', '["FRUIT"]'),
('Spinach', 'Palak', 'SAFE', 'Excellent source of folate, iron and calcium — essential in pregnancy', NULL, '[1,2,3]', '["VEGETABLE","LEAFY_GREEN"]'),
('Jaggery', 'Gur', 'SAFE', 'Rich in iron; helps prevent anemia; better than refined sugar for blood sugar management', 'Moderate amounts', '[1,2,3]', '["SWEETENER"]'),
('Fenugreek seeds', 'Methi Dana', 'CAUTION', 'May stimulate uterine contractions in large amounts; small cooking amounts safe', 'Small culinary amounts only', '[2,3]', '["SPICE","SEED"]'),
('Turmeric', 'Haldi', 'CAUTION', 'Culinary use is safe; medicinal/supplemental doses should be avoided', 'Normal cooking amounts only', '[1,2,3]', '["SPICE"]'),
('Ginger', 'Adrak', 'SAFE', 'Helps with nausea and morning sickness; anti-inflammatory; widely used in Indian cooking', 'Up to 1g/day', '[1,2,3]', '["SPICE"]'),
('Ajinomoto (MSG)', 'Ajinomoto', 'AVOID', 'May cross the placenta; no established safe level in pregnancy', NULL, '[1,2,3]', '["ADDITIVE"]'),
('Dal (Lentils)', 'Dal', 'SAFE', 'Excellent plant-based protein and folate source; highly recommended', NULL, '[1,2,3]', '["LEGUME","PROTEIN"]'),
('Almonds', 'Badam', 'SAFE', 'Rich in Vitamin E, calcium, and healthy fats; supports brain development', 'Handful (20-25) per day', '[1,2,3]', '["NUT"]'),
('Raw sprouts', 'Ankurit Anaj', 'AVOID', 'High risk of Salmonella and E. coli contamination; do not eat raw', NULL, '[1,2,3]', '["VEGETABLE"]'),
('Unpasteurized milk', 'Kaccha Doodh', 'AVOID', 'Risk of Listeria, Salmonella and other pathogens; always boil milk before drinking', NULL, '[1,2,3]', '["DAIRY"]'),
('Curd/Yogurt (pasteurized)', 'Dahi', 'SAFE', 'Excellent calcium and probiotic source; supports digestion', NULL, '[1,2,3]', '["DAIRY"]'),
('Sesame seeds (black/white)', 'Til', 'CAUTION', 'Traditionally avoided in first trimester due to emmenagogue properties; safe in cooking amounts later', 'Avoid large amounts in T1', '[2,3]', '["SEED"]');
