-- create sample table
CREATE TABLE test_taxi_table (
    trip_id VARCHAR,
    VendorID VARCHAR,
    tpep_pickup_datetime TIMESTAMP,
    tpep_dropoff_datetime TIMESTAMP,
    passenger_count BIGINT,
    fare_amount REAL,
    PRIMARY KEY (trip_id)
);

-- verify that data was inserted correctly
SELECT * FROM test_taxi_table;