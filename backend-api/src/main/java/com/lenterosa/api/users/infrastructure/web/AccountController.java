package com.lenterosa.api.users.infrastructure.web;

import com.lenterosa.api.users.application.AccountService;
import com.lenterosa.api.users.domain.Account;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cuenta de la usuaria autenticada (RF-02). Siempre requiere un ID token de Firebase.
 *
 * <p>Privacidad: la respuesta solo incluye el UID, si el correo está verificado (según el token,
 * sin guardarlo) y la fecha de alta. El nombre y el apodo del token se ignoran.
 */
@RestController
@RequestMapping("/api/v1/me")
class AccountController {

    private final AccountService accounts;

    AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    /** Registra la cuenta la primera vez que la usuaria inicia sesión; es idempotente. */
    @PutMapping
    AccountResponse register(@AuthenticationPrincipal Jwt token) {
        return AccountResponse.from(accounts.register(token.getSubject()), token);
    }

    @GetMapping
    ResponseEntity<?> current(@AuthenticationPrincipal Jwt token) {
        return accounts.find(token.getSubject())
                .<ResponseEntity<?>>map(account -> ResponseEntity.ok(AccountResponse.from(account, token)))
                .orElseGet(() -> {
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                            "Tu cuenta todavía no está registrada en Lente Rosa.");
                    problem.setTitle("Cuenta no registrada");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
                });
    }

    /**
     * Elimina la cuenta en la API: sus opiniones quedan anónimas. La app pide reautenticación y
     * borra la cuenta en Firebase.
     */
    @DeleteMapping
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt token) {
        accounts.delete(token.getSubject());
        return ResponseEntity.noContent().build();
    }

    record AccountResponse(String uid, boolean emailVerified, Instant registeredAt) {

        static AccountResponse from(Account account, Jwt token) {
            return new AccountResponse(account.uid(), Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified")),
                    account.registeredAt());
        }
    }
}
