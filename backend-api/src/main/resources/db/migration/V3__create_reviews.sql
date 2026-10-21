-- Opiniones sobre un lugar: dimensiones de 1 a 5, el periodo de la visita y un comentario opcional.
-- La experiencia nocturna es obligatoria en visitas de noche y opcional en visitas de día.
CREATE TABLE reviews (
    id                     uuid         PRIMARY KEY,
    place_id               uuid         NOT NULL REFERENCES places (id),
    -- UID de Firebase. Nulo hasta el día 4 y al anonimizar una cuenta eliminada; nunca se expone en la API.
    author_id              varchar(128),
    lighting               smallint     NOT NULL,
    women_presence         smallint     NOT NULL,
    commercial_visibility  smallint     NOT NULL,
    night_experience       smallint,
    visit_period           varchar(8)   NOT NULL,
    comment                varchar(500),
    created_at             timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT reviews_lighting_range              CHECK (lighting BETWEEN 1 AND 5),
    CONSTRAINT reviews_women_presence_range        CHECK (women_presence BETWEEN 1 AND 5),
    CONSTRAINT reviews_commercial_visibility_range CHECK (commercial_visibility BETWEEN 1 AND 5),
    CONSTRAINT reviews_night_experience_range      CHECK (night_experience BETWEEN 1 AND 5),
    CONSTRAINT reviews_visit_period_valid          CHECK (visit_period IN ('DAY', 'NIGHT')),
    CONSTRAINT reviews_night_experience_required   CHECK (visit_period = 'DAY' OR night_experience IS NOT NULL),
    CONSTRAINT reviews_comment_not_blank           CHECK (comment IS NULL OR btrim(comment) <> '')
);

-- Opiniones de un lugar, de la más reciente a la más antigua, y promedios por lugar.
CREATE INDEX reviews_place_created_idx ON reviews (place_id, created_at DESC);
