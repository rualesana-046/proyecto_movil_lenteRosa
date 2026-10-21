package com.lenterosa.api.places.infrastructure.persistence;

import com.lenterosa.api.places.domain.GeoPoint;
import com.lenterosa.api.places.domain.Place;
import com.lenterosa.api.places.domain.PlaceCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/** Fila de la tabla {@code places}. */
@Entity
@Table(name = "places")
class PlaceEntity {

    private static final int WGS84 = 4326;
    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), WGS84);

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "address")
    private String address;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(name = "location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlaceEntity() {
        // Requerido por JPA.
    }

    static PlaceEntity fromDomain(Place place) {
        PlaceEntity entity = new PlaceEntity();
        entity.id = place.id();
        entity.name = place.name();
        entity.category = place.category().name();
        entity.address = place.address();
        entity.location = toPoint(place.location());
        entity.createdAt = place.createdAt();
        return entity;
    }

    Place toDomain() {
        return new Place(id, name, PlaceCategory.valueOf(category), address, toGeoPoint(location), createdAt);
    }

    // JTS y PostGIS usan (x = longitud, y = latitud). Este es el único lugar donde se convierte.
    static Point toPoint(GeoPoint geoPoint) {
        return GEOMETRY.createPoint(new Coordinate(geoPoint.longitude(), geoPoint.latitude()));
    }

    static GeoPoint toGeoPoint(Point point) {
        return new GeoPoint(point.getY(), point.getX());
    }
}
