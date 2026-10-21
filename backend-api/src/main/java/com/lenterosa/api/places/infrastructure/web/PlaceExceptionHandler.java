package com.lenterosa.api.places.infrastructure.web;

import com.lenterosa.api.places.application.PlaceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
}
