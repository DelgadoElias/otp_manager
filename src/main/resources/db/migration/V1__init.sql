CREATE TABLE IF NOT EXISTS otp_entries (
    id        BIGSERIAL    PRIMARY KEY,
    username  VARCHAR(255) NOT NULL UNIQUE,
    secret    VARCHAR(255) NOT NULL,
    expiration TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS otp_audit (
    id        BIGSERIAL    PRIMARY KEY,
    username  VARCHAR(255) NOT NULL,
    action    VARCHAR(50)  NOT NULL,
    timestamp TIMESTAMP    NOT NULL
);