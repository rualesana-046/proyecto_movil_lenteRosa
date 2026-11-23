package com.lenterosa.api.users.application;

import com.lenterosa.api.reviews.ReviewAuthorship;
import com.lenterosa.api.users.domain.Account;
import java.time.Clock;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de la cuenta de la usuaria autenticada (RF-02).
 *
 * <p>Se registra como bean en la capa de infraestructura. Usa {@link Transactional} porque eliminar
 * una cuenta toca dos módulos y debe hacerse completo o no hacerse.
 */
public class AccountService {

    private final AccountRepository accounts;
    private final ReviewAuthorship reviewAuthorship;
    private final Clock clock;

    public AccountService(AccountRepository accounts, ReviewAuthorship reviewAuthorship, Clock clock) {
        this.accounts = accounts;
        this.reviewAuthorship = reviewAuthorship;
        this.clock = clock;
    }

    /** Registra la cuenta la primera vez; las siguientes veces devuelve la existente. */
    @Transactional
    public Account register(String uid) {
        return accounts.registerIfAbsent(uid, clock.instant());
    }

    @Transactional(readOnly = true)
    public Optional<Account> find(String uid) {
        return accounts.findByUid(uid);
    }

    /**
     * Elimina la cuenta: sus opiniones quedan anónimas (no se borran) y se borra el registro
     * (Ley 1581 de 2012). Es idempotente: eliminar una cuenta que no existe no es un error.
     */
    @Transactional
    public void delete(String uid) {
        reviewAuthorship.anonymizeReviewsBy(uid);
        accounts.delete(uid);
    }
}
