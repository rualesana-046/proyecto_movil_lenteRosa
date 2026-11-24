package com.lenterosa.api.alerts.infrastructure;

import com.lenterosa.api.alerts.application.AlertRepository;
import com.lenterosa.api.alerts.application.AlertService;
import com.lenterosa.api.places.ReviewContributed;
import com.lenterosa.api.users.SavedPlaceQueries;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/** Registra las alertas y las conecta a las opiniones publicadas. */
@Configuration(proxyBeanMethods = false)
class AlertsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AlertsConfiguration.class);

    @Bean
    AlertService alertService(AlertRepository alertRepository, SavedPlaceQueries savedPlaceQueries, Clock clock) {
        return new AlertService(alertRepository, savedPlaceQueries, clock);
    }

    @Bean
    ReviewContributedListener reviewContributedListener(AlertService alertService) {
        return new ReviewContributedListener(alertService);
    }

    /**
     * Escucha cada opinión publicada. Si crear los avisos falla, la opinión igual queda publicada:
     * el aviso es secundario. En el registro no se escribe quién opinó.
     */
    static class ReviewContributedListener {

        private final AlertService alerts;

        ReviewContributedListener(AlertService alerts) {
            this.alerts = alerts;
        }

        @EventListener
        void on(ReviewContributed event) {
            try {
                alerts.onReviewContributed(event);
            } catch (RuntimeException e) {
                log.warn("No se pudieron crear los avisos del lugar {}: {}", event.placeId(), e.getClass().getSimpleName());
            }
        }
    }
}
