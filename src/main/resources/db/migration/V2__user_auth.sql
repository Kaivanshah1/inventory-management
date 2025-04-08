CREATE TABLE UserAuth (
    id VARCHAR(255) PRIMARY KEY,
    userId VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    hashPassword VARCHAR(255) NOT NULL,
    roles TEXT[],
    createdAt BIGINT
);

CREATE TABLE refresh_tokens (
    id VARCHAR(255) PRIMARY KEY,
    userId VARCHAR(255) NOT NULL,
    token VARCHAR(512) NOT NULL,
    expiresAt BIGINT,
    createdAt BIGINT,
    FOREIGN KEY (userId) REFERENCES UserAuth(userId)
);