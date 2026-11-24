package com.lenterosa.api.places.infrastructure.web;

import com.lenterosa.api.places.application.PlaceNotFoundException;
import com.lenterosa.api.reviews.ReviewRateLimitedException;
import com.lenterosa.api.reviews.ReviewRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores propios de la consulta de lugares, en formato RFC 9457. */
@RestControllerAdvice(basePackageClasses = PlaceController.class)
class PlaceExceptionHandler {

    @ExceptionHandler(PlaceNotFoundException.class)
    ProblemDetail placeNotFound(PlaceNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "No encontramos ese lugar. Puede que el enlace esté desactualizado.");
        problem.setTitle("Lugar no encontrado");
        return problem;
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    ProblemDetail emailNotVerified(EmailNotVerifiedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Verifica tu correo para poder opinar. Revisa el enlace que te enviamos.");
        problem.setTitle("Correo sin verificar");
        return problem;
    }

    @ExceptionHandler(ReviewRejectedException.class)
    ProblemDetail reviewRejected(ReviewRejectedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Opinión no válida");
        problem.setProperty("reason", exception.reason().name());
        return problem;
    }

    // 429 con Retry-After en segundos, redondeado hacia arriba.
    @ExceptionHandler(ReviewRateLimitedException.class)
    ResponseEntity<ProblemDetail> rateLimited(ReviewRateLimitedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
        problem.setTitle("Límite de opiniones");
        long seconds = Math.max(1, (exception.retryAfter().toMillis() + 999) / 1000);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(seconds))
                .body(problem);
    }
}
