ALTER TABLE participation
    ADD UNIQUE INDEX uk_participation_pickup_token (pickup_token);
