package com.lenterosa.api.comfortindex.infrastructure;

import com.lenterosa.api.comfortindex.application.ComfortIndexService;
import com.lenterosa.api.reviews.ReviewQueries;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra el cálculo del índice, que no depende de Spring. */
@Configuration(proxyBeanMethods = false)
class ComfortIndexConfiguration {

    @Bean
    ComfortIndexService comfortIndexService(ReviewQueries reviewQueries, Clock clock) {
        return new ComfortIndexService(reviewQueries, clock);
    }
}
