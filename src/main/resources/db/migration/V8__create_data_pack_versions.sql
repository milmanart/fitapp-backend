CREATE TABLE data_pack_versions (
    pack_name VARCHAR(64) PRIMARY KEY,
    version INT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO data_pack_versions(pack_name, version) VALUES
  ('dish_templates', 0),
  ('lang_en', 0),
  ('lang_pl', 0),
  ('mini_usda', 0)
ON CONFLICT (pack_name) DO NOTHING;
