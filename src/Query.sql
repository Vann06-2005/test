-- 1. Users Table (Matches UserController.java)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(100),
    password VARCHAR(255),
    role VARCHAR(50) -- e.g., 'ADMIN' or 'CUSTOMER'
);

-- 2. Buses Table (Matches BusController.java)
CREATE TABLE IF NOT EXISTS buses (
    id SERIAL PRIMARY KEY,
    bus_number VARCHAR(50), -- e.g., 'BUS-101'
    total_seats INT,
    type VARCHAR(50),       -- e.g., 'Sleeper'
    is_operational BOOLEAN
);

-- 3. Routes Table (Matches RouteController.java)
CREATE TABLE IF NOT EXISTS routes (
    id SERIAL PRIMARY KEY,
    source_city VARCHAR(100),
    destination_city VARCHAR(100),
    distance_km DOUBLE PRECISION, 
    estimated_duration VARCHAR(50) -- e.g., '6 hours'
);

-- 4. Schedules Table (Matches ScheduleController.java)
CREATE TABLE IF NOT EXISTS schedules (
    id SERIAL PRIMARY KEY,
    bus_id INT REFERENCES buses(id),    -- Foreign Key to buses
    route_id INT REFERENCES routes(id), -- Foreign Key to routes
    departure_time TIMESTAMP,
    arrival_time TIMESTAMP,
    ticket_price DECIMAL(10, 2),        -- Matches BigDecimal
    available_seats INT
);

-- 5. Bookings Table (Matches BookingController.java)
CREATE TABLE IF NOT EXISTS bookings (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id),         -- Foreign Key to users
    schedule_id INT REFERENCES schedules(id), -- Foreign Key to schedules
    seat_number VARCHAR(10),
    status VARCHAR(20),                       -- e.g., 'CONFIRMED'
    total_amount DECIMAL(10, 2),
    booking_date TIMESTAMP,
    CONSTRAINT unique_seat_per_schedule UNIQUE (schedule_id, seat_number) -- Prevents double booking
);

-- Insert 15 Users
INSERT INTO users (full_name, password, role) VALUES
('Vannara', 'admin123', 'ADMIN'),
('Sok Piseth', 'pass123', 'CUSTOMER'),
('Chan Vuthy', 'pass123', 'CUSTOMER'),
('Dara Rith', 'pass123', 'CUSTOMER'),
('Bopha Devi', 'pass123', 'CUSTOMER'),
('Mony Oudom', 'pass123', 'DRIVER'),
('Chea Sothea', 'pass123', 'CUSTOMER'),
('Heng Visal', 'pass123', 'CUSTOMER'),
('Keo Sophea', 'pass123', 'CUSTOMER'),
('Leng Virak', 'pass123', 'CUSTOMER'),
('Nary Roth', 'pass123', 'CUSTOMER'),
('Ouk Samnang', 'pass123', 'ADMIN'),
('Pich Sovan', 'pass123', 'CUSTOMER'),
('Rathana Phirun', 'pass123', 'CUSTOMER'),
('Samnang Sopheak', 'pass123', 'CUSTOMER');

-- Insert 15 Buses
INSERT INTO buses (bus_number, total_seats, type, is_operational) VALUES
('BUS-PP-001', 40, 'AC Seater', true),
('BUS-PP-002', 30, 'VIP Sleeper', true),
('BUS-SR-003', 45, 'Non-AC Seater', true),
('BUS-SHV-004', 40, 'AC Seater', true),
('BUS-BTB-005', 30, 'VIP Sleeper', true),
('BUS-KPT-006', 20, 'Minivan', true),
('BUS-KEP-007', 20, 'Minivan', true),
('BUS-KCH-008', 40, 'AC Seater', true),
('BUS-KKO-009', 30, 'VIP Sleeper', false),
('BUS-PP-010', 45, 'Luxury', true),
('BUS-PP-011', 40, 'AC Seater', true),
('BUS-PP-012', 30, 'VIP Sleeper', true),
('BUS-SR-013', 45, 'Non-AC Seater', true),
('BUS-SHV-014', 40, 'AC Seater', true),
('BUS-BTB-015', 30, 'VIP Sleeper', true);

-- Insert 15 Routes
INSERT INTO routes (source_city, destination_city, distance_km, estimated_duration) VALUES
('Phnom Penh', 'Siem Reap', 314.5, '5h 30m'),
('Phnom Penh', 'Sihanoukville', 230.0, '4h 00m'),
('Phnom Penh', 'Battambang', 291.2, '5h 00m'),
('Phnom Penh', 'Kampot', 148.0, '3h 00m'),
('Phnom Penh', 'Kep', 164.5, '3h 30m'),
('Phnom Penh', 'Koh Kong', 271.0, '5h 00m'),
('Phnom Penh', 'Mondulkiri', 381.0, '6h 30m'),
('Phnom Penh', 'Ratanakiri', 488.0, '8h 00m'),
('Siem Reap', 'Phnom Penh', 314.5, '5h 30m'),
('Siem Reap', 'Battambang', 173.0, '3h 00m'),
('Sihanoukville', 'Phnom Penh', 230.0, '4h 00m'),
('Sihanoukville', 'Kampot', 100.0, '2h 00m'),
('Battambang', 'Phnom Penh', 291.2, '5h 00m'),
('Kampot', 'Phnom Penh', 148.0, '3h 00m'),
('Phnom Penh', 'Poipet', 400.0, '7h 00m');
-- Insert 15 Schedules
INSERT INTO schedules (bus_id, route_id, departure_time, arrival_time, ticket_price, available_seats) VALUES
(1, 1, '2025-12-25 08:00:00', '2025-12-25 13:30:00', 12.00, 35),
(2, 2, '2025-12-25 22:00:00', '2025-12-26 02:00:00', 15.50, 28),
(3, 3, '2025-12-25 09:00:00', '2025-12-25 14:00:00', 10.00, 40),
(4, 4, '2025-12-25 14:00:00', '2025-12-25 17:00:00', 8.50, 38),
(5, 5, '2025-12-26 08:00:00', '2025-12-26 11:30:00', 9.00, 25),
(6, 6, '2025-12-26 10:00:00', '2025-12-26 15:00:00', 11.00, 18),
(7, 7, '2025-12-26 07:00:00', '2025-12-26 13:30:00', 14.00, 18),
(8, 8, '2025-12-26 18:00:00', '2025-12-27 02:00:00', 16.50, 35),
(9, 9, '2025-12-27 08:00:00', '2025-12-27 13:30:00', 12.00, 30),
(10, 10, '2025-12-27 12:00:00', '2025-12-27 15:00:00', 8.00, 42),
(11, 11, '2025-12-27 14:00:00', '2025-12-27 18:00:00', 12.00, 38),
(12, 12, '2025-12-28 08:00:00', '2025-12-28 10:00:00', 6.50, 28),
(13, 13, '2025-12-28 09:00:00', '2025-12-28 14:00:00', 10.00, 44),
(14, 14, '2025-12-28 13:00:00', '2025-12-28 16:00:00', 8.00, 36),
(15, 15, '2025-12-29 07:00:00', '2025-12-29 10:00:00', 8.00, 29);

-- Insert 15 Bookings
INSERT INTO bookings (user_id, schedule_id, seat_number, status, total_amount, booking_date) VALUES
(2, 1, '1', 'CONFIRMED', 12.00, '2025-12-20 10:00:00'),
(3, 2, '2', 'CONFIRMED', 15.50, '2025-12-20 11:00:00'),
(4, 3, '3', 'CONFIRMED', 10.00, '2025-12-21 12:00:00'),
(5, 4, '4', 'CONFIRMED', 8.50, '2025-12-21 13:00:00'),
(7, 5, '5', 'CANCELLED', 9.00, '2025-12-22 09:00:00'),
(8, 6, '6', 'CONFIRMED', 11.00, '2025-12-22 10:00:00'),
(9, 7, '7', 'CONFIRMED', 14.00, '2025-12-22 11:00:00'),
(10, 8, '8', 'CONFIRMED', 16.50, '2025-12-23 14:00:00'),
(11, 9, '9', 'PENDING', 12.00, '2025-12-23 15:00:00'),
(13, 10, '10', 'CONFIRMED', 8.00, '2025-12-23 16:00:00'),
(14, 11, '11', 'CONFIRMED', 12.00, '2025-12-24 08:00:00'),
(15, 12, '12', 'CONFIRMED', 6.50, '2025-12-24 09:00:00'),
(2, 13, '13', 'CANCELLED', 10.00, '2025-12-24 10:00:00'),
(3, 14, '14', 'CONFIRMED', 8.00, '2025-12-25 11:00:00'),
(4, 15, '15', 'CONFIRMED', 8.00, '2025-12-25 12:00:00');

Select * FROM users;	
Select * FROM schedules;
Select * FROM buses;
Select * FROM routes;
Select * FROM bookings;