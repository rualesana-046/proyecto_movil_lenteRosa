package com.lenterosa.api.reviews.infrastructure;

import com.lenterosa.api.reviews.application.ReviewAuthorshipService;
import com.lenterosa.api.reviews.application.ReviewQueryService;
import com.lenterosa.api.reviews.application.ReviewSubmissionService;
import com.lenterosa.api.reviews.application.ReviewRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de opiniones, que no dependen de Spring. */
@Configuration(proxyBeanMethods = false)
class ReviewsConfiguration {

    @Bean
    ReviewQueryService reviewQueryService(ReviewRepository reviewRepository) {
        return new ReviewQueryService(reviewRepository);
    }

    @Bean
    ReviewAuthorshipService reviewAuthorshipService(ReviewRepository reviewRepository) {
        return new ReviewAuthorshipService(reviewRepository);
    }

    @Bean
    ReviewSubmissionService reviewSubmissionService(ReviewRepository reviewRepository, Clock clock) {
        return new ReviewSubmissionService(reviewRepository, clock);
    }
}
