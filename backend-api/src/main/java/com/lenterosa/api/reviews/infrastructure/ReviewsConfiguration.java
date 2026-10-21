package com.lenterosa.api.reviews.infrastructure;

import com.lenterosa.api.reviews.application.ReviewQueryService;
import com.lenterosa.api.reviews.application.ReviewRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de opiniones, que no dependen de Spring. */
@Configuration(proxyBeanMethods = false)
class ReviewsConfiguration {

    @Bean
    ReviewQueryService reviewQueryService(ReviewRepository reviewRepository) {
        return new ReviewQueryService(reviewRepository);
    }
}
