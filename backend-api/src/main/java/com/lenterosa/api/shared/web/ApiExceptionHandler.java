package com.lenterosa.api.shared.web;

import java.util.List;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Errores de solicitud comunes a toda la API, en formato RFC 9457 ({@link ProblemDetail})
 * y con mensajes en lenguaje cotidiano. Nunca se recorta un valor en silencio: si un parámetro
 * no es válido, se responde 400 explicando por qué.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    static final String INVALID_REQUEST_TITLE = "Solicitud no válida";

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail invalidParameters(HandlerMethodValidationException exception) {
        List<String> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .distinct()
                .toList();
        return badRequest(errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail missingParameter(MissingServletRequestParameterException exception) {
        return badRequest(List.of("Falta el parámetro " + exception.getParameterName() + "."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail wrongType(MethodArgumentTypeMismatchException exception) {
        return badRequest(List.of("El valor de " + exception.getName() + " no tiene el formato esperado."));
    }

    private static ProblemDetail badRequest(List<String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, String.join(" ", errors));
        problem.setTitle(INVALID_REQUEST_TITLE);
        problem.setProperty("errors", errors);
        return problem;
    }
}
