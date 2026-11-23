package com.lenterosa.api.comfortindex.application;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.comfortindex.ComfortIndexQueries;
import com.lenterosa.api.comfortindex.domain.ComfortIndexCalculator;
import com.lenterosa.api.comfortindex.domain.RatingAggregates;
import com.lenterosa.api.reviews.DecayedRatingSums;
import com.lenterosa.api.reviews.ReviewQueries;
import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calcula el Índice de Confort al leer: siempre refleja la última opinión y el paso del tiempo,
 * sin procesos programados. Las sumas ponderadas por antigüedad las hace la base (una fila por
 * lugar); la fórmula se aplica aquí, en el dominio.
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
        Map<UUID, DecayedRatingSums> sumsByPlace = reviews
                .decayedRatingSums(placeIds, clock.instant(), ComfortIndexCalculator.HALF_LIFE_DAYS).stream()
                .collect(Collectors.toMap(DecayedRatingSums::placeId, Function.identity()));
        Map<UUID, ComfortIndex> result = new LinkedHashMap<>();
        for (UUID placeId : placeIds) {
            DecayedRatingSums sums = sumsByPlace.get(placeId);
            RatingAggregates aggregates = sums == null ? RatingAggregates.empty() : toAggregates(sums);
            result.put(placeId, ComfortIndexCalculator.calculate(aggregates));
        }
        return result;
    }

    private static RatingAggregates toAggregates(DecayedRatingSums sums) {
        return new RatingAggregates(
                sums.reviewCount(),
                sums.totalWeight(),
                sums.lightingWeightedSum(),
                sums.womenPresenceWeightedSum(),
                sums.commercialVisibilityWeightedSum(),
                sums.nightExperienceWeight(),
                sums.nightExperienceWeightedSum(),
                sums.reviewMeanWeightedSum(),
                sums.reviewMeanSquaredWeightedSum());
    }
}
