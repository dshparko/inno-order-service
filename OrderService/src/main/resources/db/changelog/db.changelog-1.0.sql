--liquibase formatted sql

--changeset dshparko:1
CREATE TABLE IF NOT EXISTS orders
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGSERIAL,
    status        VARCHAR(32) NOT NULL,
    creation_date DATE        NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);

--changeset dshparko:2
CREATE TABLE IF NOT EXISTS items
(
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(128)   NOT NULL,
    price NUMERIC(10, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_items_name ON items (name);

--changeset dshparko:3

CREATE TABLE IF NOT EXISTS order_items
(
    id       BIGSERIAL PRIMARY KEY,
    order_id BIGINT  NOT NULL REFERENCES orders (id),
    item_id  BIGINT  NOT NULL REFERENCES items (id),
    quantity INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_item_id ON order_items (item_id);
