package com.lenterosa.api.places.infrastructure.web;

import com.lenterosa.api.places.application.NearbySearch;
import com.lenterosa.api.places.application.PlaceQueryService;
import com.lenterosa.api.places.domain.GeoPoint;
import com.lenterosa.api.reviews.PublicReview;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta pública de lugares, sin iniciar sesión.
 *
 * <p>Privacidad: {@code lat} y {@code lng} son la ubicación que la persona comparte para esta
 * consulta. Se usan solo para calcular y nunca se guardan ni se escriben en los logs.
 */
@RestController
@RequestMapping("/api/v1/places")
class PlaceController {

    private final PlaceQueryService places;

    PlaceController(PlaceQueryService places) {
        this.places = places;
    }

    @GetMapping
    PageResponse<PlaceSummaryResponse> search(
            @RequestParam
            @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
            @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.")
            double lat,
            @RequestParam
            @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
            @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.")
            double lng,
            @RequestParam(defaultValue = "" + NearbySearch.DEFAULT_RADIUS_METERS)
            @Min(value = 1, message = "El radio de búsqueda debe estar entre 1 y 5000 metros.")
            @Max(value = NearbySearch.MAX_RADIUS_METERS, message = "El radio de búsqueda debe estar entre 1 y 5000 metros.")
            int radiusMeters,
            @RequestParam(required = false)
            @Size(max = NearbySearch.MAX_NAME_QUERY_LENGTH, message = "La búsqueda por nombre puede tener hasta 120 caracteres.")
            String q,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "El número de página no puede ser negativo.")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Cada página puede tener entre 1 y 50 resultados.")
            @Max(value = PlaceQueryService.MAX_PAGE_SIZE, message = "Cada página puede tener entre 1 y 50 resultados.")
            int size) {
        NearbySearch search = new NearbySearch(new GeoPoint(lat, lng), radiusMeters, q);
        return PageResponse.from(places.searchNearby(search, PageRequest.of(page, size)), PlaceSummaryResponse::from);
    }

    @GetMapping("/{id}")
    PlaceDetailResponse detail(@PathVariable UUID id) {
        return PlaceDetailResponse.from(places.getDetail(id));
    }

    /** Opiniones de un lugar, de la más reciente a la más antigua, siempre anónimas. */
    @GetMapping("/{id}/reviews")
    PageResponse<PublicReview> reviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "El número de página no puede ser negativo.")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Cada página puede tener entre 1 y 50 resultados.")
            @Max(value = PlaceQueryService.MAX_PAGE_SIZE, message = "Cada página puede tener entre 1 y 50 resultados.")
            int size) {
        return PageResponse.from(places.listReviews(id, PageRequest.of(page, size)), review -> review);
    }
}
