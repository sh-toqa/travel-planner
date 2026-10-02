CREATE TABLE users (
    id       UUID         NOT NULL,
    email    VARCHAR(255) NOT NULL,
    name     VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email)
);