-- Lugares guardados (RF-05). Solo el UID de la usuaria, el lugar y la fecha. Al eliminar la cuenta
-- o el lugar, la fila se borra sola (Ley 1581 de 2012).
CREATE TABLE saved_places (
    user_id     varchar(128) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    place_id    uuid         NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, place_id)
);

-- Para encontrar rápido quién guardó un lugar cuando llega una opinión nueva (RF-07).
CREATE INDEX saved_places_place_idx ON saved_places (place_id);
