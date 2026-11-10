ALTER TABLE store_holidays
    MODIFY COLUMN day_of_week INT NULL;

ALTER TABLE store_pickup_time_slots
    MODIFY COLUMN day_of_week INT NOT NULL;
