-- Migration V2: Enhance schema for User Stories implementation
-- Version: 2.0.0

-- Enable UUID extension (already enabled in V1)

-- ============================================
-- USERS TABLE ENHANCEMENTS (US01, US08)
-- ============================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS cnpj VARCHAR(18) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS establishment_name VARCHAR(200);
ALTER TABLE users ADD COLUMN IF NOT EXISTS establishment_address VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS establishment_city VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS establishment_state VARCHAR(2);
ALTER TABLE users ADD COLUMN IF NOT EXISTS establishment_zip_code VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_user_cnpj ON users(cnpj);
CREATE INDEX IF NOT EXISTS idx_user_locked_until ON users(locked_until);

-- ============================================
-- COLLECTION POINTS ENHANCEMENTS (US11)
-- ============================================
ALTER TABLE collection_points ADD COLUMN IF NOT EXISTS alert_almost_full BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE collection_points ADD COLUMN IF NOT EXISTS alert_triggered_at TIMESTAMP;

-- Add index for almost full alerts
CREATE INDEX IF NOT EXISTS idx_point_alert_almost_full ON collection_points(alert_almost_full) WHERE alert_almost_full = true;

-- ============================================
-- COLLECTION SCHEDULES ENHANCEMENTS (US02, US03, US06)
-- ============================================
ALTER TABLE collection_schedules ADD COLUMN IF NOT EXISTS sale_type VARCHAR(20) NOT NULL DEFAULT 'SALE';
ALTER TABLE collection_schedules ADD COLUMN IF NOT EXISTS time_window VARCHAR(50);
ALTER TABLE collection_schedules ADD COLUMN IF NOT EXISTS base_price_per_liter DECIMAL(10, 2);
ALTER TABLE collection_schedules ADD COLUMN IF NOT EXISTS calculated_value DECIMAL(10, 2);
ALTER TABLE collection_schedules ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP;
ALTER TABLE collection_schedules ADD COLUMN IF NOT EXISTS en_route_at TIMESTAMP;

-- Add check constraint for sale_type
ALTER TABLE collection_schedules ADD CONSTRAINT chk_sale_type CHECK (sale_type IN ('SALE', 'EXCHANGE'));

-- Add index for new columns
CREATE INDEX IF NOT EXISTS idx_schedule_sale_type ON collection_schedules(sale_type);
CREATE INDEX IF NOT EXISTS idx_schedule_assigned_at ON collection_schedules(assigned_at);

-- ============================================
-- COLLECTION RECORDS ENHANCEMENTS (US05, US07, US14)
-- ============================================
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS actual_volume_liters DECIMAL(10, 2);
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS confirmation_code VARCHAR(20);
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS establishment_confirmed BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS establishment_confirmed_at TIMESTAMP;
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS establishment_confirmed_by UUID REFERENCES users(id);
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS verified_by UUID REFERENCES users(id);
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS discrepancy_flag BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS discrepancy_reason TEXT;
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS certificate_code VARCHAR(50) UNIQUE;
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS certificate_generated_at TIMESTAMP;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_record_certificate_code ON collection_records(certificate_code);
CREATE INDEX IF NOT EXISTS idx_record_discrepancy ON collection_records(discrepancy_flag) WHERE discrepancy_flag = true;
CREATE INDEX IF NOT EXISTS idx_record_establishment_confirmed_by ON collection_records(establishment_confirmed_by);
CREATE INDEX IF NOT EXISTS idx_record_verified_by ON collection_records(verified_by);

-- ============================================
-- NEW TABLE: PRODUCTS (US06)
-- ============================================
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    unit VARCHAR(50) NOT NULL,
    price_per_unit DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    min_stock_quantity INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_product_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_product_is_active ON products(is_active);

-- ============================================
-- NEW TABLE: EXCHANGE_ITEMS (US06)
-- ============================================
CREATE TABLE IF NOT EXISTS exchange_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    schedule_id UUID NOT NULL REFERENCES collection_schedules(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_exchange_schedule ON exchange_items(schedule_id);

-- ============================================
-- NEW TABLE: INCIDENTS (US15)
-- ============================================
CREATE TABLE IF NOT EXISTS incidents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    collection_point_id UUID NOT NULL REFERENCES collection_points(id),
    schedule_id UUID REFERENCES collection_schedules(id),
    reporter_id UUID NOT NULL REFERENCES users(id),
    incident_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'LOW',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by UUID REFERENCES users(id),
    resolution_notes TEXT,
    photos JSONB,
    gps_latitude DECIMAL(10, 8),
    gps_longitude DECIMAL(11, 8)
);

CREATE INDEX IF NOT EXISTS idx_incident_point ON incidents(collection_point_id);
CREATE INDEX IF NOT EXISTS idx_incident_schedule ON incidents(schedule_id);
CREATE INDEX IF NOT EXISTS idx_incident_status ON incidents(status);
CREATE INDEX IF NOT EXISTS idx_incident_reported_at ON incidents(reported_at);

-- ============================================
-- NEW TABLE: AUDIT_LOGS (US14)
-- ============================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    changed_by UUID REFERENCES users(id),
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_changed_by ON audit_logs(changed_by);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);

-- ============================================
-- NEW TABLE: NOTIFICATIONS (US03, US12)
-- ============================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivery_method VARCHAR(20) NOT NULL DEFAULT 'IN_APP'
);

CREATE INDEX IF NOT EXISTS idx_notification_user ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notification_sent_at ON notifications(sent_at);

-- ============================================
-- NEW TABLE: COLLECTOR_COVERAGE (US04)
-- ============================================
CREATE TABLE IF NOT EXISTS collector_coverage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    collector_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    neighborhoods JSONB,
    max_distance_km DECIMAL(5, 2),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(collector_id, city, state)
);

CREATE INDEX IF NOT EXISTS idx_coverage_collector ON collector_coverage(collector_id);
CREATE INDEX IF NOT EXISTS idx_coverage_city_state ON collector_coverage(city, state);

-- ============================================
-- NEW TABLE: SYSTEM_CONFIG (US05, US06, US11)
-- ============================================
CREATE TABLE IF NOT EXISTS system_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description TEXT,
    data_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_config (config_key, config_value, description, data_type) VALUES
    ('base_oil_price_per_liter', '2.50', 'Preço base do litro de óleo para venda', 'DECIMAL'),
    ('discrepancy_threshold_percentage', '20', 'Limite percentual de discrepância entre estimado e real', 'INTEGER'),
    ('cancellation_hours_before', '2', 'Horas antes do agendamento para cancelamento pelo estabelecimento', 'INTEGER'),
    ('account_lockout_max_attempts', '5', 'Máximo de tentativas de login antes de bloquear conta', 'INTEGER'),
    ('account_lockout_duration_minutes', '30', 'Duração do bloqueio da conta em minutos', 'INTEGER'),
    ('password_min_length', '8', 'Tamanho mínimo da senha', 'INTEGER'),
    ('password_require_uppercase', 'true', 'Exigir letra maiúscula na senha', 'BOOLEAN'),
    ('password_require_lowercase', 'true', 'Exigir letra minúscula na senha', 'BOOLEAN'),
    ('password_require_number', 'true', 'Exigir número na senha', 'BOOLEAN'),
    ('password_require_special', 'true', 'Exigir caractere especial na senha', 'BOOLEAN')
ON CONFLICT (config_key) DO NOTHING;

-- ============================================
-- DEFAULT PRODUCTS (US06)
-- ============================================
INSERT INTO products (name, description, category, unit, price_per_unit, stock_quantity, min_stock_quantity, is_active) VALUES
    ('Detergente Neutro 5L', 'Detergente neutro concentrado 5 litros', 'LIMPEZA', 'UN', 25.00, 100, 10, true),
    ('Desinfetante 5L', 'Desinfetante bactericida 5 litros', 'LIMPEZA', 'UN', 30.00, 80, 10, true),
    ('Sabão em Pó 1kg', 'Sabão em pó para roupas 1kg', 'LIMPEZA', 'UN', 15.00, 150, 20, true),
    ('Álcool 70% 1L', 'Álcool etílico 70% 1 litro', 'HIGIENE', 'UN', 12.00, 200, 20, true),
    ('Limpador Multiuso 5L', 'Limpador multiuso concentrado 5 litros', 'LIMPEZA', 'UN', 28.00, 60, 10, true),
    ('Desengraxante 5L', 'Desengraxante industrial 5 litros', 'LIMPEZA', 'UN', 35.00, 50, 5, true),
    ('Pano de Chão', 'Pano de chão microfibra', 'LIMPEZA', 'UN', 8.00, 300, 50, true),
    ('Vassoura', 'Vassoura de piaçava', 'LIMPEZA', 'UN', 18.00, 100, 10, true)
ON CONFLICT DO NOTHING;