CREATE TABLE IF NOT EXISTS category (
    category_number INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS product (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    category_number INT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(100) NOT NULL,
    characteristics VARCHAR(255) NOT NULL,
    CONSTRAINT fk_product_category
    FOREIGN KEY (category_number) REFERENCES category(category_number)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);