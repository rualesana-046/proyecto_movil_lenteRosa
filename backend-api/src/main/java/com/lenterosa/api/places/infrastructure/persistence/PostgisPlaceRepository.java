package com.lenterosa.api.places.infrastructure.persistence;

import com.lenterosa.api.places.application.NearbyPlace;
import com.lenterosa.api.places.application.NearbySearch;
import com.lenterosa.api.places.application.PlaceRepository;
import com.lenterosa.api.places.domain.GeoPoint;
import com.lenterosa.api.places.domain.Place;
import com.lenterosa.api.places.domain.PlaceCategory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador del puerto {@link PlaceRepository}: JPA para leer por id y SQL nativo de PostGIS
 * solo para la búsqueda por distancia.
 */
@Repository
@Transactional(readOnly = true)
class PostgisPlaceRepository implements PlaceRepository {

    // El centro se construye como (longitud, latitud): es el orden de ST_MakePoint.
    private static final String NEARBY_FROM_WHERE = """
            FROM places p,
                 (SELECT ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography AS point) center
            WHERE ST_DWithin(p.location, center.point, :radiusMeters)
              AND (CAST(:namePattern AS text) IS NULL OR p.name ILIKE CAST(:namePattern AS text) ESCAPE '\\')
            """;

    private static final String NEARBY_PAGE_SQL = """
            SELECT p.id, p.name, p.category, p.address, p.created_at,
                   ST_Y(p.location::geometry) AS latitude,
                   ST_X(p.location::geometry) AS longitude,
                   ST_Distance(p.location, center.point) AS distance_meters
            """ + NEARBY_FROM_WHERE + """
            ORDER BY distance_meters, p.id
            LIMIT :limit OFFSET :offset
            """;

    private static final String NEARBY_COUNT_SQL = "SELECT count(*) " + NEARBY_FROM_WHERE;

    private final PlaceJpaRepository jpa;
    private final JdbcClient jdbc;

    PostgisPlaceRepository(PlaceJpaRepository jpa, JdbcClient jdbc) {
        this.jpa = jpa;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Place> findById(UUID id) {
        return jpa.findById(id).map(PlaceEntity::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public Page<NearbyPlace> findNearby(NearbySearch search, Pageable pageable) {
        String namePattern = search.nameQuery() == null ? null : "%" + escapeLike(search.nameQuery()) + "%";

        List<NearbyPlace> content = jdbc.sql(NEARBY_PAGE_SQL)
                .param("longitude", search.center().longitude())
                .param("latitude", search.center().latitude())
                .param("radiusMeters", search.radiusMeters())
                .param("namePattern", namePattern)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, rowNum) -> toNearbyPlace(rs))
                .list();
        long total = jdbc.sql(NEARBY_COUNT_SQL)
                .param("longitude", search.center().longitude())
                .param("latitude", search.center().latitude())
                .param("radiusMeters", search.radiusMeters())
                .param("namePattern", namePattern)
                .query(Long.class)
                .single();
        return new PageImpl<>(content, pageable, total);
    }

    // El texto de la persona se busca literal: %, _ y \ no actúan como comodines.
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static NearbyPlace toNearbyPlace(ResultSet rs) throws SQLException {
        Place place = new Place(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                PlaceCategory.valueOf(rs.getString("category")),
                rs.getString("address"),
                new GeoPoint(rs.getDouble("latitude"), rs.getDouble("longitude")),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
        return new NearbyPlace(place, rs.getDouble("distance_meters"));
    }
}
