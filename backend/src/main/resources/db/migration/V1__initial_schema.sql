-- Initial schema for Oil Collection System
-- Version: 1.0.0

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'COLLECTOR',
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Collection Points table
CREATE TABLE collection_points (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    address VARCHAR(255) NOT NULL,
    neighborhood VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    zip_code VARCHAR(10),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    capacity_liters INTEGER,
    current_volume_liters INTEGER NOT NULL DEFAULT 0,
    contact_name VARCHAR(100),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    operating_hours JSONB,
    accepted_oil_types JSONB,
    collector_id UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Collection Schedules table
CREATE TABLE collection_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    collection_point_id UUID NOT NULL REFERENCES collection_points(id),
    collector_id UUID NOT NULL REFERENCES users(id),
    scheduled_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    estimated_volume_liters INTEGER,
    notes TEXT,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Collection Records table
CREATE TABLE collection_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    collection_point_id UUID NOT NULL REFERENCES collection_points(id),
    collector_id UUID NOT NULL REFERENCES users(id),
    schedule_id UUID REFERENCES collection_schedules(id),
    collection_date TIMESTAMP NOT NULL,
    volume_collected_liters DECIMAL(10, 2) NOT NULL,
    oil_type VARCHAR(50),
    container_count INTEGER,
    container_type VARCHAR(50),
    quality_rating INTEGER,
    temperature_celsius DECIMAL(5, 2),
    ph_level DECIMAL(3, 1),
    contamination_percentage DECIMAL(5, 2),
    photos JSONB,
    notes TEXT,
    gps_latitude DECIMAL(10, 8),
    gps_longitude DECIMAL(11, 8),
    verified_by_manager BOOLEAN NOT NULL DEFAULT false,
    verified_at TIMESTAMP,
    verified_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Users
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_cpf ON users(cpf);
CREATE INDEX idx_user_role ON users(role);
CREATE INDEX idx_user_is_active ON users(is_active);

-- Indexes for Collection Points
CREATE INDEX idx_point_status ON collection_points(status);
CREATE INDEX idx_point_collector ON collection_points(collector_id);
CREATE INDEX idx_point_city_state ON collection_points(city, state);
CREATE INDEX idx_point_location ON collection_points(latitude, longitude);

-- Indexes for Collection Schedules
CREATE INDEX idx_schedule_date ON collection_schedules(scheduled_date);
CREATE INDEX idx_schedule_collector ON collection_schedules(collector_id);
CREATE INDEX idx_schedule_point ON collection_schedules(collection_point_id);
CREATE INDEX idx_schedule_status ON collection_schedules(status);

-- Indexes for Collection Records
CREATE INDEX idx_record_date ON collection_records(collection_date);
CREATE INDEX idx_record_point ON collection_records(collection_point_id);
CREATE INDEX idx_record_collector ON collection_records(collector_id);
CREATE INDEX idx_record_schedule ON collection_records(schedule_id);
CREATE INDEX idx_record_verified ON collection_records(verified_by_manager);

-- Insert default admin user (password: admin123)
INSERT INTO users (id, name, email, password, cpf, role, is_active, created_at)
VALUES (
    uuid_generate_v4(),
    'System Administrator',
    'admin@oilcollection.com',
    '$2a$12$5B5LDD6yzdca1S.krjHLteG2Scita//1jj/PZLpbK5mS8Llz60fEK',
    '00000000000',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;