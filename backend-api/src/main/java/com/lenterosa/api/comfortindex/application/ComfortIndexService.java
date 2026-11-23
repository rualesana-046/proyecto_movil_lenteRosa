package com.lenterosa.api.comfortindex.application;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.comfortindex.ComfortIndexQueries;
import com.lenterosa.api.comfortindex.domain.ComfortIndexCalculator;
import com.lenterosa.api.comfortindex.domain.RatingSample;
import com.lenterosa.api.reviews.ReviewQueries;
import com.lenterosa.api.reviews.ReviewRatings;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calcula el Índice de Confort al leer: siempre refleja la última opinión y el paso del tiempo,
 * sin procesos programados.
 *
 * <p>Sin anotaciones de Spring: la capa de infraestructura lo registra como bean.
 */
public class ComfortIndexService implements ComfortIndexQueries {

    private final ReviewQueries reviews;
    private final Clock clock;

    public ComfortIndexService(ReviewQueries reviews, Clock clock) {
        this.reviews = reviews;
        this.clock = clock;
    }

    @Override
    public ComfortIndex forPlace(UUID placeId) {
        return forPlaces(List.of(placeId)).get(placeId);
    }

    @Override
    public Map<UUID, ComfortIndex> forPlaces(Collection<UUID> placeIds) {
        Instant now = clock.instant();
        Map<UUID, List<RatingSample>> samplesByPlace = reviews.ratingsForPlaces(placeIds).stream()
                .collect(Collectors.groupingBy(ReviewRatings::placeId,
                        Collectors.mapping(ComfortIndexService::toSample, Collectors.toList())));
        Map<UUID, ComfortIndex> result = new LinkedHashMap<>();
        for (UUID placeId : placeIds) {
            result.put(placeId, ComfortIndexCalculator.calculate(samplesByPlace.getOrDefault(placeId, List.of()), now));
        }
        return result;
    }

    private static RatingSample toSample(ReviewRatings ratings) {
        return new RatingSample(ratings.createdAt(), ratings.lighting(), ratings.womenPresence(),
                ratings.commercialVisibility(), ratings.nightExperience());
    }
}
