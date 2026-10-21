-- DATOS FICTICIOS para desarrollo local (perfil local). Ningún lugar es real.
-- Coordenadas aproximadas dentro de Pasto (Nariño). ST_MakePoint recibe (longitud, latitud).
-- Idempotente: se puede volver a ejecutar sin duplicar filas.

INSERT INTO places (id, name, category, address, location) VALUES
    ('01929f00-0000-7000-8000-000000000001', '[FICTICIO] Parque de Prueba Norte',     'PARK',      'Dirección ficticia 1', ST_SetSRID(ST_MakePoint(-77.2790, 1.2200), 4326)::geography),
    ('01929f00-0000-7000-8000-000000000002', '[FICTICIO] Parada de Bus Ejemplo A',    'TRANSPORT', 'Dirección ficticia 2', ST_SetSRID(ST_MakePoint(-77.2815, 1.2140), 4326)::geography),
    ('01929f00-0000-7000-8000-000000000003', '[FICTICIO] Tienda de Ejemplo Centro',   'COMMERCE',  'Dirección ficticia 3', ST_SetSRID(ST_MakePoint(-77.2800, 1.2130), 4326)::geography),
    ('01929f00-0000-7000-8000-000000000004', '[FICTICIO] Colegio de Prueba Sur',      'EDUCATION', 'Dirección ficticia 4', ST_SetSRID(ST_MakePoint(-77.2850, 1.2050), 4326)::geography),
    ('01929f00-0000-7000-8000-000000000005', '[FICTICIO] Centro de Salud de Ejemplo', 'HEALTH',    'Dirección ficticia 5', ST_SetSRID(ST_MakePoint(-77.2700, 1.2160), 4326)::geography),
    ('01929f00-0000-7000-8000-000000000006', '[FICTICIO] Bar de Prueba Oriente',      'NIGHTLIFE', 'Dirección ficticia 6', ST_SetSRID(ST_MakePoint(-77.2680, 1.2120), 4326)::geography),
    ('01929f00-0000-7000-8000-000000000007', '[FICTICIO] Calle de Ejemplo 1',         'STREET',    'Dirección ficticia 7', ST_SetSRID(ST_MakePoint(-77.2830, 1.2100), 4326)::geography),
    ('01929f00-0000-7000-8000-000000000008', '[FICTICIO] Lugar de Prueba Occidente',  'OTHER',     'Dirección ficticia 8', ST_SetSRID(ST_MakePoint(-77.2950, 1.2150), 4326)::geography)
ON CONFLICT (id) DO NOTHING;

-- Opiniones ficticias sin autora. Las de día pueden omitir la experiencia nocturna.
INSERT INTO reviews (id, place_id, lighting, women_presence, commercial_visibility, night_experience, visit_period, comment, created_at) VALUES
    ('01929f00-0000-7000-8000-000000000101', '01929f00-0000-7000-8000-000000000001', 4, 4, 3, NULL, 'DAY',   'Comentario ficticio: buena iluminación de día.', now() - interval '20 days'),
    ('01929f00-0000-7000-8000-000000000102', '01929f00-0000-7000-8000-000000000001', 2, 2, 2, 2,    'NIGHT', 'Comentario ficticio: poca gente de noche.',      now() - interval '3 days'),
    ('01929f00-0000-7000-8000-000000000103', '01929f00-0000-7000-8000-000000000002', 3, 3, 4, 3,    'NIGHT', NULL,                                              now() - interval '7 days'),
    ('01929f00-0000-7000-8000-000000000104', '01929f00-0000-7000-8000-000000000003', 5, 4, 5, NULL, 'DAY',   NULL,                                              now() - interval '1 day'),
    ('01929f00-0000-7000-8000-000000000105', '01929f00-0000-7000-8000-000000000007', 1, 2, 1, 1,    'NIGHT', 'Comentario ficticio: tramo oscuro.',             now() - interval '10 days'),
    ('01929f00-0000-7000-8000-000000000106', '01929f00-0000-7000-8000-000000000006', 3, 4, 4, 4,    'NIGHT', NULL,                                              now() - interval '2 days')
ON CONFLICT (id) DO NOTHING;
