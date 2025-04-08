CREATE TABLE bill (
    id VARCHAR(255) PRIMARY KEY,
    customer_name VARCHAR(255),
    customer_phone_no VARCHAR(20),
    status VARCHAR(50),
    created_at BIGINT
);

CREATE TABLE billitems (
    id VARCHAR(36) PRIMARY KEY,
    b_id VARCHAR(36) NOT NULL,
    item_id VARCHAR(36),
    quantity INT,
    rate DOUBLE PRECISION,
    tax DECIMAL(5,2),
    created_at BIGINT,
    FOREIGN KEY (b_id) REFERENCES bill(id) ON DELETE CASCADE
);