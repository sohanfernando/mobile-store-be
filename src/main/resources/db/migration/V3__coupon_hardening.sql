ALTER TABLE coupons
    ADD COLUMN max_uses INT NULL,
    ADD COLUMN uses_count INT NOT NULL DEFAULT 0,
    ADD COLUMN min_order_amount DECIMAL(10,2) NULL;

ALTER TABLE orders
    ADD COLUMN coupon_code VARCHAR(50) NULL,
    ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0;

CREATE TABLE coupon_redemptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT NOT NULL,
    customer_email VARCHAR(150) NOT NULL,
    order_id BIGINT NOT NULL,
    redeemed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
