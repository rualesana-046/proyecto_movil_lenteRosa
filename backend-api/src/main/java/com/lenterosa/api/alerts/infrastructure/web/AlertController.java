package com.lenterosa.api.alerts.infrastructure.web;

import com.lenterosa.api.alerts.application.Alert;
import com.lenterosa.api.alerts.application.AlertService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Avisos de cambio en los lugares guardados de la usuaria autenticada (RF-07). */
@RestController
@RequestMapping("/api/v1/me/alerts")
class AlertController {

    private final AlertService alerts;

    AlertController(AlertService alerts) {
        this.alerts = alerts;
    }

    @GetMapping
    InboxResponse inbox(@AuthenticationPrincipal Jwt token) {
        AlertService.AlertInbox inbox = alerts.inbox(token.getSubject());
        return new InboxResponse(inbox.unreadCount(), inbox.items().stream().map(AlertResponse::from).toList());
    }

    /** Marca todos los avisos como leídos (la app lo hace al mostrarlos). */
    @PutMapping("/read")
    ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt token) {
        alerts.markAllRead(token.getSubject());
        return ResponseEntity.noContent().build();
    }

    record InboxResponse(int unreadCount, List<AlertResponse> items) {
    }

    /** Etiquetas como códigos en inglés; la app muestra el texto en español. */
    record AlertResponse(UUID id, UUID placeId, String previousLabel, Integer previousIndex, String newLabel,
            Integer newIndex, Instant createdAt, boolean read) {

        static AlertResponse from(Alert alert) {
            return new AlertResponse(alert.id(), alert.placeId(), alert.previous().label().name(), alert.previous().index(),
                    alert.current().label().name(), alert.current().index(), alert.createdAt(), alert.read());
        }
    }
}
