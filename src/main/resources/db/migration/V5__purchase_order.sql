CREATE TABLE purchase_orders (
    id VARCHAR(255) PRIMARY KEY,
    vendor_id VARCHAR(255) NOT NULL,
    status VARCHAR(20),
    expected_date BIGINT,
    created_at BIGINT,
    FOREIGN KEY (vendor_id) REFERENCES vendor(id)
);

CREATE TABLE purchase_order_items (
    id  VARCHAR(255) PRIMARY KEY,
    po_id VARCHAR(255) NOT NULL,
    item_id VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    rate DECIMAL(10,2) NOT NULL,
    created_at BIGINT,
    FOREIGN KEY (po_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES item(id)
);