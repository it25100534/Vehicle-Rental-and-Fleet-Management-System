CREATE TABLE IF NOT EXISTS branches (
    branch_id VARCHAR(30) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN','CLOSED')),
    image_path VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS vehicle_categories (
    code VARCHAR(30) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
    username VARCHAR(80) PRIMARY KEY,
    full_name VARCHAR(140),
    license_id VARCHAR(80) NOT NULL UNIQUE,
    email VARCHAR(160) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    license_verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS staff_accounts (
    user_id VARCHAR(40) PRIMARY KEY,
    full_name VARCHAR(140) NOT NULL,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','STAFF')),
    employee_type VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    home_branch_id VARCHAR(30),
    FOREIGN KEY (home_branch_id) REFERENCES branches(branch_id)
);

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id VARCHAR(40) PRIMARY KEY,
    make VARCHAR(80) NOT NULL,
    model VARCHAR(80) NOT NULL,
    manufacture_year INT NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL,
    fuel_type VARCHAR(30) NOT NULL,
    seat_count INT,
    vehicle_subtype VARCHAR(40),
    category_code VARCHAR(30) NOT NULL,
    branch_id VARCHAR(30) NOT NULL,
    rental_rate DECIMAL(12,2) NOT NULL,
    mileage DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL CHECK (status IN ('AVAILABLE','BOOKED','RENTED','MAINTENANCE','UNAVAILABLE','RETIRED')),
    image_path VARCHAR(500),
    image_hash VARCHAR(64),
    retired BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    FOREIGN KEY (category_code) REFERENCES vehicle_categories(code)
);

CREATE TABLE IF NOT EXISTS bookings (
    transaction_id VARCHAR(50) PRIMARY KEY,
    customer_username VARCHAR(80) NOT NULL,
    vehicle_id VARCHAR(40) NOT NULL,
    start_date DATE NOT NULL,
    return_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    pickup_branch_id VARCHAR(30),
    dropoff_branch_id VARCHAR(30),
    extras_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    promo_code VARCHAR(30),
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    alternate_dropoff_charge DECIMAL(12,2) NOT NULL DEFAULT 0,
    cancellation_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    cancellation_reason VARCHAR(255),
    cancelled_at TIMESTAMP,
    CONSTRAINT chk_bookings_status CHECK (status IN ('Pending','Approved','Rejected','Cancelled','Paid','Active','Returned','Completed')),
    FOREIGN KEY (customer_username) REFERENCES customers(username),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
    FOREIGN KEY (pickup_branch_id) REFERENCES branches(branch_id),
    FOREIGN KEY (dropoff_branch_id) REFERENCES branches(branch_id),
    INDEX idx_bookings_vehicle_dates (vehicle_id, start_date, return_date)
);

CREATE TABLE IF NOT EXISTS maintenance_records (
    record_id VARCHAR(50) PRIMARY KEY,
    vehicle_id VARCHAR(40) NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    scheduled_date DATE NOT NULL,
    expected_completion_date DATE NOT NULL,
    completed_date DATE,
    provider VARCHAR(160) NOT NULL,
    estimated_cost DECIMAL(12,2) NOT NULL DEFAULT 0,
    actual_cost DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL CHECK (status IN ('Pending','In Progress','Completed','Cancelled')),
    notes VARCHAR(500),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
    INDEX idx_maintenance_vehicle_date (vehicle_id, scheduled_date)
);

CREATE TABLE IF NOT EXISTS fleet_compliance (
    vehicle_id VARCHAR(40) PRIMARY KEY,
    insurance_expiry DATE NOT NULL,
    licence_expiry DATE NOT NULL,
    emission_expiry DATE NOT NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

CREATE TABLE IF NOT EXISTS rentals (
    record_id VARCHAR(50) PRIMARY KEY,
    booking_id VARCHAR(50) NOT NULL UNIQUE,
    customer_username VARCHAR(80) NOT NULL,
    vehicle_id VARCHAR(40) NOT NULL,
    scheduled_return_date DATE NOT NULL,
    handed_over_at TIMESTAMP NOT NULL,
    odometer_out DECIMAL(12,2) NOT NULL,
    fuel_level_out VARCHAR(40) NOT NULL,
    condition_out VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','RETURNED','VOID')),
    returned_at TIMESTAMP,
    odometer_in DECIMAL(12,2),
    fuel_level_in VARCHAR(40),
    condition_in VARCHAR(500),
    late_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    damage_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    fuel_charge DECIMAL(12,2) NOT NULL DEFAULT 0,
    mileage_charge DECIMAL(12,2) NOT NULL DEFAULT 0,
    alternate_dropoff_charge DECIMAL(12,2) NOT NULL DEFAULT 0,
    deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (booking_id) REFERENCES bookings(transaction_id),
    FOREIGN KEY (customer_username) REFERENCES customers(username),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

CREATE TABLE IF NOT EXISTS rental_reviews (
    booking_id VARCHAR(50) PRIMARY KEY,
    customer_username VARCHAR(80) NOT NULL,
    stars INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    review_text VARCHAR(1000),
    source VARCHAR(12) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES bookings(transaction_id),
    FOREIGN KEY (customer_username) REFERENCES customers(username)
);

CREATE TABLE IF NOT EXISTS invoices (
    invoice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id VARCHAR(50),
    customer_username VARCHAR(80) NOT NULL,
    vehicle_id VARCHAR(40) NOT NULL,
    branch_id VARCHAR(30),
    rental_days INT NOT NULL,
    amount_per_day DECIMAL(12,2) NOT NULL,
    discount DECIMAL(12,2) NOT NULL DEFAULT 0,
    late_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    damage_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    fuel_charge DECIMAL(12,2) NOT NULL DEFAULT 0,
    mileage_charge DECIMAL(12,2) NOT NULL DEFAULT 0,
    alternate_dropoff_charge DECIMAL(12,2) NOT NULL DEFAULT 0,
    deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING','PAID','CANCELLED','PARTIALLY_REFUNDED','REFUNDED','VOID')),
    payment_type VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    voided_at TIMESTAMP,
    void_reason VARCHAR(255),
    refunded_at TIMESTAMP,
    refund_reason VARCHAR(255),
    FOREIGN KEY (booking_id) REFERENCES bookings(transaction_id),
    FOREIGN KEY (customer_username) REFERENCES customers(username),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    INDEX idx_invoices_customer (customer_username)
);

CREATE TABLE IF NOT EXISTS activity_log (
    activity_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    module_name VARCHAR(40) NOT NULL,
    actor VARCHAR(80) NOT NULL,
    action_name VARCHAR(40) NOT NULL,
    record_reference VARCHAR(80),
    details VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    INDEX idx_activity_module_time (module_name, occurred_at)
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id VARCHAR(50) PRIMARY KEY,
    booking_id VARCHAR(50),
    invoice_id BIGINT,
    idempotency_key VARCHAR(100) UNIQUE,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PAID',
    failure_reason VARCHAR(255),
    paid_at TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(transaction_id),
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
);

CREATE TABLE IF NOT EXISTS contact_enquiries (
    enquiry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_username VARCHAR(80),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    subject VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (customer_username) REFERENCES customers(username)
);

CREATE TABLE IF NOT EXISTS rental_inspection_audit (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rental_id VARCHAR(50) NOT NULL,
    action_name VARCHAR(30) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    actor VARCHAR(80) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    FOREIGN KEY (rental_id) REFERENCES rentals(record_id)
);
