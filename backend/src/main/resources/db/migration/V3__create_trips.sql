CREATE TABLE trips (
   id                   UUID         NOT NULL,
   owner_id             UUID         NOT NULL,
   title                VARCHAR(100),
   destination          VARCHAR(200) NOT NULL,
   start_date           DATE         NOT NULL,
   end_date             DATE         NOT NULL,
   budget_level         VARCHAR(20)  NOT NULL,
   pace                 VARCHAR(20)  NOT NULL,
   vibes                TEXT[]       NOT NULL,
   must_see             TEXT,
   special_requirements TEXT,
   version              BIGINT       NOT NULL DEFAULT 0,
   created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
   updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),

   CONSTRAINT trips_pkey PRIMARY KEY (id),
    -- A trip has no meaning without its owner.
    CONSTRAINT fk_trips_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_trips_dates CHECK (end_date >= start_date),
    -- Max 14 days (end date inclusive); bounds AI cost and output size.
   CONSTRAINT ck_trips_max_length CHECK (end_date - start_date < 14),
   CONSTRAINT ck_trips_budget_level CHECK (budget_level IN ('BUDGET', 'BALANCED', 'PREMIUM')),
   CONSTRAINT ck_trips_pace CHECK (pace IN ('RELAXED', 'MODERATE', 'ACTIVE')),
    -- Ordered: the first vibe is the primary one.
   CONSTRAINT ck_trips_vibes_count CHECK (cardinality(vibes) BETWEEN 1 AND 3),
   CONSTRAINT ck_trips_vibes_values CHECK (vibes <@ ARRAY ['ADVENTURE', 'CULTURE', 'FOOD', 'ROMANTIC', 'NATURE', 'URBAN']::TEXT[])
);

-- Every trip query filters by owner.
CREATE INDEX ix_trips_owner_id ON trips (owner_id);