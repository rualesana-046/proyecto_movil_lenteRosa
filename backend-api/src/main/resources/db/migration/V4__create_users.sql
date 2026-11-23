-- Cuentas registradas. Solo se guarda el UID de Firebase y la fecha de alta: ni correo, ni nombre,
-- ni apodo (el apodo vive solo en Firebase como displayName).
CREATE TABLE users (
    id          varchar(128) PRIMARY KEY,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT users_id_not_blank CHECK (btrim(id) <> '')
);
