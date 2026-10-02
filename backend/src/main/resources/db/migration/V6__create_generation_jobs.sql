-- One row per itinerary generation request; the background worker moves it through its states.
CREATE TABLE generation_jobs (
                                 id            UUID        NOT NULL,
                                 trip_id       UUID        NOT NULL,
                                 requested_by  UUID        NOT NULL,
                                 status        VARCHAR(20) NOT NULL,
                                 error_code    VARCHAR(50),
                                 error_message TEXT,
                                 error_details TEXT[]      NOT NULL DEFAULT '{}',
                                 created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 started_at    TIMESTAMPTZ,
                                 finished_at   TIMESTAMPTZ,

                                 CONSTRAINT generation_jobs_pkey PRIMARY KEY (id),
                                 CONSTRAINT fk_generation_jobs_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
                                 CONSTRAINT fk_generation_jobs_user FOREIGN KEY (requested_by) REFERENCES users (id) ON DELETE CASCADE,
                                 CONSTRAINT ck_generation_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX ix_generation_jobs_trip_id ON generation_jobs (trip_id);

-- At most one active generation per trip, even when two requests arrive at the same moment.
CREATE UNIQUE INDEX ux_generation_jobs_active_trip ON generation_jobs (trip_id)
    WHERE status IN ('PENDING', 'RUNNING');