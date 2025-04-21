-- Table for high-demand zones
CREATE TABLE high_demand_zones (
    Zone VARCHAR(255),
    pickup_hour INT,
    pickup_day VARCHAR(50),
    total_trips INT,
    avg_fare NUMERIC,
    avg_passenger_count NUMERIC,
    avg_trip_duration NUMERIC,
    avg_fare_per_minute NUMERIC,
    rank INT
);

-- Table for demand trends
CREATE TABLE demand_trends (
    Zone VARCHAR(255),
    pickup_day VARCHAR(50),
    weekly_trips INT,
    weekly_avg_fare NUMERIC
);

-- Table for shared ride pricing
CREATE TABLE shared_ride_pricing (
    Zone VARCHAR(255),
    pickup_interval VARCHAR(100),
    total_trips INT,
    total_passengers INT,
    avg_fare NUMERIC,
    shared_ride_feasibility INT,
    dynamic_price_multiplier NUMERIC,
    final_price NUMERIC
);