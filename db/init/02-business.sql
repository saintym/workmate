-- =============================================================================
-- 02-business.sql  –  E-commerce demo schema + seed data for Text-to-SQL (Phase 2)
-- Runs after 01-init.sql on first Postgres container init.
-- To apply manually to an already-running container use:  db/apply-business.sh
-- =============================================================================

-- Demo workspace id used for ALL seed rows so demo queries are reproducible.
-- DEMO_WORKSPACE_ID = 00000000-0000-0000-0000-000000000001

-- =============================================================================
-- SCHEMA
-- =============================================================================

CREATE TABLE IF NOT EXISTS customers (
    id           UUID         PRIMARY KEY,
    workspace_id UUID         NOT NULL,
    name         TEXT         NOT NULL,
    email        TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS products (
    id           UUID         PRIMARY KEY,
    workspace_id UUID         NOT NULL,
    name         TEXT         NOT NULL,
    category     TEXT         NOT NULL,
    price_cents  INT          NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS orders (
    id           UUID         PRIMARY KEY,
    workspace_id UUID         NOT NULL,
    customer_id  UUID         NOT NULL REFERENCES customers(id),
    product_id   UUID         NOT NULL REFERENCES products(id),
    quantity     INT          NOT NULL,
    total_cents  INT          NOT NULL,
    status       TEXT         NOT NULL,
    ordered_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS refunds (
    id           UUID         PRIMARY KEY,
    workspace_id UUID         NOT NULL,
    order_id     UUID         NOT NULL REFERENCES orders(id),
    product_id   UUID         NOT NULL REFERENCES products(id),
    reason       TEXT,
    amount_cents INT          NOT NULL,
    refunded_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_customers_workspace  ON customers(workspace_id);
CREATE INDEX IF NOT EXISTS idx_products_workspace   ON products(workspace_id);
CREATE INDEX IF NOT EXISTS idx_orders_workspace     ON orders(workspace_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer      ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_product       ON orders(product_id);
CREATE INDEX IF NOT EXISTS idx_refunds_workspace    ON refunds(workspace_id);
CREATE INDEX IF NOT EXISTS idx_refunds_order        ON refunds(order_id);
CREATE INDEX IF NOT EXISTS idx_refunds_product      ON refunds(product_id);

-- =============================================================================
-- SEED DATA
-- All rows use workspace_id = 00000000-0000-0000-0000-000000000001 (demo workspace).
-- Fixed UUIDs + ON CONFLICT DO NOTHING makes the script idempotent.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Products  (8 items, 3 categories: Electronics, Clothing, Home)
-- -----------------------------------------------------------------------------
INSERT INTO products (id, workspace_id, name, category, price_cents, created_at) VALUES
  ('a1000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Wireless Headphones Pro',   'Electronics', 12900, now() - INTERVAL '120 days'),
  ('a1000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Bluetooth Speaker Mini',    'Electronics',  5900, now() - INTERVAL '120 days'),
  ('a1000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'USB-C Charging Hub',        'Electronics',  3900, now() - INTERVAL '120 days'),
  ('a1000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'Smart LED Desk Lamp',       'Electronics',  4900, now() - INTERVAL '120 days'),
  ('a1000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'Classic Cotton T-Shirt',    'Clothing',     2900, now() - INTERVAL '120 days'),
  ('a1000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001', 'Slim-Fit Chino Pants',      'Clothing',     5900, now() - INTERVAL '120 days'),
  ('a1000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001', 'Bamboo Cutting Board Set',  'Home',         3500, now() - INTERVAL '120 days'),
  ('a1000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000001', 'Stainless Steel Water Bottle','Home',       2500, now() - INTERVAL '120 days')
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Customers  (6 customers)
-- -----------------------------------------------------------------------------
INSERT INTO customers (id, workspace_id, name, email, created_at) VALUES
  ('b1000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Alice Kim',    'alice@example.com',   now() - INTERVAL '90 days'),
  ('b1000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Bob Lee',      'bob@example.com',     now() - INTERVAL '85 days'),
  ('b1000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Carol Park',   'carol@example.com',   now() - INTERVAL '80 days'),
  ('b1000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'David Choi',   'david@example.com',   now() - INTERVAL '75 days'),
  ('b1000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'Eva Jung',     'eva@example.com',     now() - INTERVAL '70 days'),
  ('b1000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001', 'Frank Yoon',   'frank@example.com',   now() - INTERVAL '65 days')
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Orders  (~30 orders spread over last 3 months)
-- Statuses: completed, shipped, cancelled
-- Fixed UUIDs ensure refunds can reference real order ids.
-- -----------------------------------------------------------------------------
INSERT INTO orders (id, workspace_id, customer_id, product_id, quantity, total_cents, status, ordered_at) VALUES
  -- Month -3 (oldest)
  ('c1000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000001','a1000000-0000-0000-0000-000000000001',1, 12900,'completed', now()-INTERVAL '85 days'),
  ('c1000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000002','a1000000-0000-0000-0000-000000000002',2, 11800,'completed', now()-INTERVAL '83 days'),
  ('c1000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000003','a1000000-0000-0000-0000-000000000001',1, 12900,'completed', now()-INTERVAL '80 days'),
  ('c1000000-0000-0000-0000-000000000004','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000004','a1000000-0000-0000-0000-000000000005',3,  8700,'completed', now()-INTERVAL '78 days'),
  ('c1000000-0000-0000-0000-000000000005','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000005','a1000000-0000-0000-0000-000000000006',1,  5900,'completed', now()-INTERVAL '75 days'),
  ('c1000000-0000-0000-0000-000000000006','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000006','a1000000-0000-0000-0000-000000000003',2,  7800,'completed', now()-INTERVAL '73 days'),
  ('c1000000-0000-0000-0000-000000000007','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000001','a1000000-0000-0000-0000-000000000007',1,  3500,'completed', now()-INTERVAL '71 days'),
  ('c1000000-0000-0000-0000-000000000008','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000002','a1000000-0000-0000-0000-000000000004',1,  4900,'completed', now()-INTERVAL '70 days'),
  ('c1000000-0000-0000-0000-000000000009','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000003','a1000000-0000-0000-0000-000000000008',2,  5000,'completed', now()-INTERVAL '68 days'),
  ('c1000000-0000-0000-0000-000000000010','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000004','a1000000-0000-0000-0000-000000000001',1, 12900,'completed', now()-INTERVAL '65 days'),
  -- Month -2
  ('c1000000-0000-0000-0000-000000000011','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000005','a1000000-0000-0000-0000-000000000001',2, 25800,'completed', now()-INTERVAL '55 days'),
  ('c1000000-0000-0000-0000-000000000012','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000006','a1000000-0000-0000-0000-000000000002',1,  5900,'completed', now()-INTERVAL '53 days'),
  ('c1000000-0000-0000-0000-000000000013','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000001','a1000000-0000-0000-0000-000000000005',2,  5800,'completed', now()-INTERVAL '50 days'),
  ('c1000000-0000-0000-0000-000000000014','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000002','a1000000-0000-0000-0000-000000000006',2, 11800,'completed', now()-INTERVAL '48 days'),
  ('c1000000-0000-0000-0000-000000000015','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000003','a1000000-0000-0000-0000-000000000003',3, 11700,'completed', now()-INTERVAL '45 days'),
  ('c1000000-0000-0000-0000-000000000016','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000004','a1000000-0000-0000-0000-000000000001',1, 12900,'shipped',   now()-INTERVAL '43 days'),
  ('c1000000-0000-0000-0000-000000000017','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000005','a1000000-0000-0000-0000-000000000007',2,  7000,'completed', now()-INTERVAL '40 days'),
  ('c1000000-0000-0000-0000-000000000018','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000006','a1000000-0000-0000-0000-000000000004',1,  4900,'completed', now()-INTERVAL '38 days'),
  ('c1000000-0000-0000-0000-000000000019','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000001','a1000000-0000-0000-0000-000000000002',3, 17700,'completed', now()-INTERVAL '35 days'),
  ('c1000000-0000-0000-0000-000000000020','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000002','a1000000-0000-0000-0000-000000000008',4, 10000,'completed', now()-INTERVAL '33 days'),
  -- Month -1 (most recent)
  ('c1000000-0000-0000-0000-000000000021','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000003','a1000000-0000-0000-0000-000000000001',1, 12900,'completed', now()-INTERVAL '25 days'),
  ('c1000000-0000-0000-0000-000000000022','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000004','a1000000-0000-0000-0000-000000000005',4, 11600,'completed', now()-INTERVAL '23 days'),
  ('c1000000-0000-0000-0000-000000000023','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000005','a1000000-0000-0000-0000-000000000006',1,  5900,'cancelled', now()-INTERVAL '20 days'),
  ('c1000000-0000-0000-0000-000000000024','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000006','a1000000-0000-0000-0000-000000000003',2,  7800,'completed', now()-INTERVAL '18 days'),
  ('c1000000-0000-0000-0000-000000000025','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000001','a1000000-0000-0000-0000-000000000004',2,  9800,'completed', now()-INTERVAL '15 days'),
  ('c1000000-0000-0000-0000-000000000026','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000002','a1000000-0000-0000-0000-000000000001',1, 12900,'shipped',   now()-INTERVAL '12 days'),
  ('c1000000-0000-0000-0000-000000000027','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000003','a1000000-0000-0000-0000-000000000002',2, 11800,'completed', now()-INTERVAL '10 days'),
  ('c1000000-0000-0000-0000-000000000028','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000004','a1000000-0000-0000-0000-000000000007',3, 10500,'completed', now()-INTERVAL '8  days'),
  ('c1000000-0000-0000-0000-000000000029','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000005','a1000000-0000-0000-0000-000000000008',2,  5000,'completed', now()-INTERVAL '5  days'),
  ('c1000000-0000-0000-0000-000000000030','00000000-0000-0000-0000-000000000001','b1000000-0000-0000-0000-000000000006','a1000000-0000-0000-0000-000000000001',1, 12900,'shipped',   now()-INTERVAL '2  days')
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Refunds  (10 refunds – concentrated on Wireless Headphones Pro and Bluetooth
-- Speaker Mini so "이번 분기 환불 1위 제품" has a clear answer)
-- -----------------------------------------------------------------------------
INSERT INTO refunds (id, workspace_id, order_id, product_id, reason, amount_cents, refunded_at) VALUES
  -- Wireless Headphones Pro (product a1...0001) – 5 refunds (clear winner)
  ('d1000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001','c1000000-0000-0000-0000-000000000001','a1000000-0000-0000-0000-000000000001','Sound quality issue',       12900, now()-INTERVAL '80 days'),
  ('d1000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000001','c1000000-0000-0000-0000-000000000003','a1000000-0000-0000-0000-000000000001','Defective unit',            12900, now()-INTERVAL '75 days'),
  ('d1000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000001','c1000000-0000-0000-0000-000000000010','a1000000-0000-0000-0000-000000000001','Changed mind',              12900, now()-INTERVAL '60 days'),
  ('d1000000-0000-0000-0000-000000000004','00000000-0000-0000-0000-000000000001','c1000000-0000-0000-0000-000000000011','a1000000-0000-0000-0000-000000000001','Connectivity problems',     12900, now()-INTERVAL '50 days'),
  ('d1000000-0000-0000-0000-000000000005','00000000-0000-0000-0000-000000000001','c1000000-0000-0000-0000-000000000021','a1000000-0000-0000-0000-000000000001','Battery not charging',      12900, now()-INTERVAL '20 days'),
  -- Bluetooth Speaker Mini (product a1...0002) – 3 refunds
  ('d1000000-0000-0000-0000-000000000006','00000000-0000-0000-0000-000000000001','c1000000-0000-0000-0000-000000000002','a1000000-0000-0000-0000-000000000002','Poor bass quality',          5900, now()-INTERVAL '78 days'),
  ('d1000000-0000-0000-0000-000000000007','00000000-0000-0000-0000-000000000001','c1000000-0000-0000-0000-000000000012','a1000000-0000-0000-0000-000000000002','Wrong colour received',      5900, now()-INTERVAL '48 days'),
  ('d1000000-0000-0000-0000-000000000008','00000000-0000-0000-0000-000000000001','c1000000-0000-0000-0000-000000000027','a1000000-0000-0000-0000-000000000002','Stopped working',            5900, now()-INTERVAL '8  days'),
  -- Classic Cotton T-Shirt (product a1...0005) – 1 refund
  ('d1000000-0000-0000-0000-000000000009','00000000-0000-0000-0000-000000000001','c1000000-0000-0000-0000-000000000004','a1000000-0000-0000-0000-000000000005','Wrong size',                 2900, now()-INTERVAL '72 days'),
  -- Slim-Fit Chino Pants (product a1...0006) – 1 refund
  ('d1000000-0000-0000-0000-000000000010','00000000-0000-0000-0000-000000000001','c1000000-0000-0000-0000-000000000014','a1000000-0000-0000-0000-000000000006','Colour faded after wash',   11800, now()-INTERVAL '43 days')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- EXAMPLE QUESTIONS THIS DATA CAN ANSWER
-- 1. "이번 분기 환불 1위 제품은?" / "Which product had the most refunds this quarter?"
--    → Wireless Headphones Pro (5 refunds, $129 × 5 = $645 refunded)
-- 2. "지난 달 총 매출은?" / "What was total revenue last month?"
--    → SUM(total_cents) from orders WHERE ordered_at >= date_trunc('month', now() - interval '1 month')
-- 3. "카테고리별 환불 건수는?" / "How many refunds per product category?"
--    → JOIN refunds → products ON product_id, GROUP BY category
-- =============================================================================
