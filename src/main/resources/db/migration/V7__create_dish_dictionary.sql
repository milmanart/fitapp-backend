CREATE TABLE dish_dictionary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    term VARCHAR(256) NOT NULL,
    dish_key VARCHAR(128) NOT NULL REFERENCES dish_templates(dish_key) ON DELETE CASCADE,
    lang VARCHAR(5) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_dish_dictionary_lang_term UNIQUE(lang, term)
);

CREATE INDEX idx_dish_dictionary_lang_term ON dish_dictionary(lang, term);
CREATE INDEX idx_dish_dictionary_term_lower ON dish_dictionary(LOWER(term));
