package com.lenterosa.api.places.infrastructure;

import com.lenterosa.api.places.application.PlaceQueryService;
import com.lenterosa.api.places.application.PlaceRepository;
import com.lenterosa.api.reviews.ReviewQueries;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de lugares, que no dependen de Spring. */
@Configuration(proxyBeanMethods = false)
class PlacesConfiguration {

    @Bean
    PlaceQueryService placeQueryService(PlaceRepository placeRepository, ReviewQueries reviewQueries) {
        return new PlaceQueryService(placeRepository, reviewQueries);
    }
}
