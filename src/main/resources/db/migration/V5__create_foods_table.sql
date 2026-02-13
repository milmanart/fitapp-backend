-- Create foods table for USDA and Open Food Facts data
CREATE TABLE foods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- External source tracking
    external_id VARCHAR(255),
    source VARCHAR(50) NOT NULL, -- 'usda', 'openfoodfacts', 'manual'
    barcode VARCHAR(50),
    
    -- Product information
    name VARCHAR(500) NOT NULL,
    brand VARCHAR(255),
    serving_size DECIMAL(10,2),
    serving_unit VARCHAR(50),
    
    -- Nutrition per 100g (standardized)
    calories DECIMAL(10,2) NOT NULL,
    protein DECIMAL(10,2) NOT NULL,
    fat DECIMAL(10,2) NOT NULL,
    carbohydrates DECIMAL(10,2) NOT NULL,
    fiber DECIMAL(10,2),
    sugar DECIMAL(10,2),
    sodium DECIMAL(10,2),
    
    -- Metadata
    language VARCHAR(10) DEFAULT 'en',
    country_codes VARCHAR(100),
    categories TEXT,
    image_url VARCHAR(500),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_synced_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT unique_external_source UNIQUE(external_id, source)
);

-- Indexes for performance
CREATE INDEX idx_foods_source ON foods(source);
CREATE INDEX idx_foods_name ON foods USING gin(to_tsvector('english', name));
CREATE INDEX idx_foods_name_lower ON foods(LOWER(name));
CREATE INDEX idx_foods_brand ON foods(brand) WHERE brand IS NOT NULL;
CREATE INDEX idx_foods_barcode ON foods(barcode) WHERE barcode IS NOT NULL;
CREATE INDEX idx_foods_updated ON foods(updated_at DESC);

-- Table for user's favorite foods
CREATE TABLE user_custom_foods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_id UUID NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    is_favorite BOOLEAN DEFAULT FALSE,
    usage_count INTEGER DEFAULT 0,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT unique_user_food UNIQUE(user_id, food_id)
);

CREATE INDEX idx_user_foods_user ON user_custom_foods(user_id);
CREATE INDEX idx_user_foods_favorites ON user_custom_foods(user_id, is_favorite) WHERE is_favorite = TRUE;
CREATE INDEX idx_user_foods_usage ON user_custom_foods(user_id, usage_count DESC);
