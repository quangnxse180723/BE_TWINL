-- Create shipments table for GHN/GHTK/SPX integrations
CREATE TABLE IF NOT EXISTS shipments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    provider VARCHAR(20) NOT NULL,
    tracking_code VARCHAR(50),
    shipping_fee NUMERIC(12, 2),
    shipping_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_shipments_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX IF NOT EXISTS idx_shipments_tracking_code ON shipments (tracking_code);
