-- Lugares que la comunidad valora. Se consultan sin iniciar sesión.
CREATE TABLE places (
    id          uuid                   PRIMARY KEY,
    name        varchar(120)           NOT NULL,
    category    varchar(32)            NOT NULL,
    address     varchar(200),
    location    geography(Point, 4326) NOT NULL,
    created_at  timestamptz            NOT NULL DEFAULT now(),
    CONSTRAINT places_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT places_category_valid CHECK (category IN (
        'TRANSPORT', 'PARK', 'COMMERCE', 'EDUCATION',
        'HEALTH', 'NIGHTLIFE', 'STREET', 'OTHER'
    ))
);

-- Índice espacial para la búsqueda por distancia (ST_DWithin).
CREATE INDEX places_location_idx ON places USING gist (location);
