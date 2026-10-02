-- Non-blocking problems found when validating a generated itinerary (e.g. a HIGH-cost activity on a BALANCED budget).
ALTER TABLE itineraries ADD COLUMN warnings TEXT[] NOT NULL DEFAULT '{}';