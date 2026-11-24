package com.lenterosa.api.users.infrastructure.web;

import com.lenterosa.api.users.application.SavedPlace;
import com.lenterosa.api.users.application.SavedPlaceLimitException;
import com.lenterosa.api.users.application.SavedPlaceService;
import com.lenterosa.api.users.application.UnknownPlaceException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lugares guardados de la usuaria autenticada (RF-05). Siempre requiere un ID token de Firebase. */
@RestController
@RequestMapping("/api/v1/me/saved-places")
class SavedPlaceController {

    private final SavedPlaceService savedPlaces;

    SavedPlaceController(SavedPlaceService savedPlaces) {
        this.savedPlaces = savedPlaces;
    }

    @GetMapping
    List<SavedPlace> list(@AuthenticationPrincipal Jwt token) {
        return savedPlaces.list(token.getSubject());
    }

    /** Guarda el lugar; guardarlo otra vez no es un error. */
    @PutMapping("/{placeId}")
    ResponseEntity<Void> save(@AuthenticationPrincipal Jwt token, @PathVariable UUID placeId) {
        savedPlaces.save(token.getSubject(), placeId);
        return ResponseEntity.noContent().build();
    }

    /** Quita el lugar; quitar uno que no estaba guardado no es un error. */
    @DeleteMapping("/{placeId}")
    ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt token, @PathVariable UUID placeId) {
        savedPlaces.remove(token.getSubject(), placeId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(UnknownPlaceException.class)
    ProblemDetail unknownPlace(UnknownPlaceException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Lugar no encontrado");
        return problem;
    }

    @ExceptionHandler(SavedPlaceLimitException.class)
    ProblemDetail limit(SavedPlaceLimitException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Límite de lugares guardados");
        return problem;
    }
}
