CREATE TABLE vendor (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    created_at BIGINT
);

CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    created_at BIGINT
);

CREATE TABLE item (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    vendor_id VARCHAR(255),
    description TEXT,
    image_url VARCHAR(255),
    created_at BIGINT,
    FOREIGN KEY (vendor_id) REFERENCES vendor(id)
);

CREATE TABLE orders (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    status VARCHAR(255),
    created_at BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_item (
    id VARCHAR(255) PRIMARY KEY,
    order_id VARCHAR(255),
    item_id VARCHAR(255),
    quantity INT,
    created_at BIGINT,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (item_id) REFERENCES item(id)
);