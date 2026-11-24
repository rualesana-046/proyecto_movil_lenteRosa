-- Avisos de cambio en lugares guardados (RF-07). Guarda las etiquetas e índices del Índice de
-- Confort antes y después, nunca quién opinó. Al eliminar la cuenta o el lugar, se borran.
CREATE TABLE alerts (
    id              uuid         PRIMARY KEY,
    user_id         varchar(128) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    place_id        uuid         NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    previous_label  varchar(32)  NOT NULL,
    new_label       varchar(32)  NOT NULL,
    previous_index  smallint,
    new_index       smallint,
    created_at      timestamptz  NOT NULL,
    read_at         timestamptz,
    CONSTRAINT alerts_previous_index_range CHECK (previous_index IS NULL OR previous_index BETWEEN 0 AND 100),
    CONSTRAINT alerts_new_index_range CHECK (new_index IS NULL OR new_index BETWEEN 0 AND 100)
);

-- Un solo aviso sin leer por usuaria y lugar: los cambios siguientes lo actualizan (avisos sutiles).
CREATE UNIQUE INDEX alerts_one_unread_per_place ON alerts (user_id, place_id) WHERE read_at IS NULL;
CREATE INDEX alerts_user_recent_idx ON alerts (user_id, created_at DESC);
