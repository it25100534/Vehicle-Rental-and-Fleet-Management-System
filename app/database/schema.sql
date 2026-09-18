-- DriveEase MySQL 8 schema for the planned database-backed version.
-- The current Java services still read and write local text files. Running this
-- script creates tables but does not move application records into MySQL.
CREATE DATABASE IF NOT EXISTS vehicle_rental_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE vehicle_rental_db;

CREATE TABLE IF NOT EXISTS branches (
  branch_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  address VARCHAR(255) NOT NULL,
  phone VARCHAR(30),
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (branch_id),
  UNIQUE KEY uq_branch_name (name),
  CONSTRAINT chk_branch_status CHECK (status IN ('OPEN', 'CLOSED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
  user_id VARCHAR(36) NOT NULL,
  username VARCHAR(80) NOT NULL,
  full_name VARCHAR(160) NOT NULL,
  email VARCHAR(255),
  phone VARCHAR(30),
  license_id VARCHAR(80),
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  employee_type VARCHAR(60),
  home_branch_id BIGINT UNSIGNED,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email (email),
  KEY idx_users_branch (home_branch_id),
  CONSTRAINT fk_users_branch FOREIGN KEY (home_branch_id) REFERENCES branches (branch_id),
  CONSTRAINT chk_user_role CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vehicles (
  vehicle_id VARCHAR(36) NOT NULL,
  vehicle_type VARCHAR(30) NOT NULL,
  make VARCHAR(80) NOT NULL,
  model VARCHAR(80) NOT NULL,
  model_year SMALLINT UNSIGNED NOT NULL,
  fuel_type VARCHAR(30),
  rental_rate DECIMAL(12,2) NOT NULL,
  mileage DECIMAL(12,1) NOT NULL DEFAULT 0,
  image_file_name VARCHAR(255),
  status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
  branch_id BIGINT UNSIGNED,
  PRIMARY KEY (vehicle_id),
  KEY idx_vehicles_branch_status (branch_id, status),
  CONSTRAINT fk_vehicles_branch FOREIGN KEY (branch_id) REFERENCES branches (branch_id),
  CONSTRAINT chk_vehicle_rate CHECK (rental_rate >= 0),
  CONSTRAINT chk_vehicle_mileage CHECK (mileage >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bookings (
  booking_id VARCHAR(36) NOT NULL,
  customer_id VARCHAR(36),
  customer_name VARCHAR(160) NOT NULL,
  vehicle_id VARCHAR(36) NOT NULL,
  start_date DATE NOT NULL,
  return_date DATE NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  pickup_branch_id BIGINT UNSIGNED,
  return_branch_id BIGINT UNSIGNED,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (booking_id),
  KEY idx_bookings_vehicle_dates (vehicle_id, start_date, return_date),
  KEY idx_bookings_customer (customer_id),
  CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES users (user_id),
  CONSTRAINT fk_bookings_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id),
  CONSTRAINT fk_bookings_pickup FOREIGN KEY (pickup_branch_id) REFERENCES branches (branch_id),
  CONSTRAINT fk_bookings_return FOREIGN KEY (return_branch_id) REFERENCES branches (branch_id),
  CONSTRAINT chk_booking_dates CHECK (return_date >= start_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS invoices (
  invoice_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  booking_id VARCHAR(36),
  customer_name VARCHAR(160) NOT NULL,
  vehicle_id VARCHAR(36) NOT NULL,
  vehicle_name VARCHAR(160),
  rental_days INT UNSIGNED NOT NULL,
  amount_per_day DECIMAL(12,2) NOT NULL,
  discount DECIMAL(12,2) NOT NULL DEFAULT 0,
  late_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  payment_method VARCHAR(30),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at TIMESTAMP NULL,
  PRIMARY KEY (invoice_id),
  KEY idx_invoices_booking (booking_id),
  CONSTRAINT fk_invoices_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id),
  CONSTRAINT fk_invoices_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id),
  CONSTRAINT chk_invoice_days CHECK (rental_days > 0),
  CONSTRAINT chk_invoice_amounts CHECK (amount_per_day >= 0 AND discount >= 0 AND late_fee >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
  payment_id VARCHAR(36) NOT NULL,
  invoice_id BIGINT UNSIGNED NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  payment_method VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  paid_at TIMESTAMP NULL,
  PRIMARY KEY (payment_id),
  KEY idx_payments_invoice (invoice_id),
  CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (invoice_id),
  CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS maintenance_records (
  record_id VARCHAR(36) NOT NULL,
  vehicle_id VARCHAR(36) NOT NULL,
  vehicle_type VARCHAR(30),
  service_type VARCHAR(120) NOT NULL,
  service_date DATE NOT NULL,
  status VARCHAR(30) NOT NULL,
  notes TEXT,
  PRIMARY KEY (record_id),
  KEY idx_maintenance_vehicle_date (vehicle_id, service_date),
  CONSTRAINT fk_maintenance_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id)
) ENGINE=InnoDB;

-- Planned handover/return workflow; no matching Java service exists yet.
CREATE TABLE IF NOT EXISTS rental_handovers (
  handover_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  booking_id VARCHAR(36) NOT NULL,
  recorded_by_user_id VARCHAR(36),
  handed_over_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  odometer_out DECIMAL(12,1) NOT NULL,
  fuel_level_out VARCHAR(30),
  condition_notes TEXT,
  PRIMARY KEY (handover_id),
  UNIQUE KEY uq_handover_booking (booking_id),
  CONSTRAINT fk_handover_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id),
  CONSTRAINT fk_handover_user FOREIGN KEY (recorded_by_user_id) REFERENCES users (user_id),
  CONSTRAINT chk_handover_odometer CHECK (odometer_out >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rental_returns (
  return_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  booking_id VARCHAR(36) NOT NULL,
  recorded_by_user_id VARCHAR(36),
  returned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  odometer_in DECIMAL(12,1) NOT NULL,
  fuel_level_in VARCHAR(30),
  condition_notes TEXT,
  damage_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
  late_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (return_id),
  UNIQUE KEY uq_return_booking (booking_id),
  CONSTRAINT fk_return_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id),
  CONSTRAINT fk_return_user FOREIGN KEY (recorded_by_user_id) REFERENCES users (user_id),
  CONSTRAINT chk_return_values CHECK (odometer_in >= 0 AND damage_fee >= 0 AND late_fee >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS activity_logs (
  activity_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actor VARCHAR(80) NOT NULL,
  action VARCHAR(40) NOT NULL,
  details TEXT,
  PRIMARY KEY (activity_id),
  KEY idx_activity_occurred (occurred_at)
) ENGINE=InnoDB;
