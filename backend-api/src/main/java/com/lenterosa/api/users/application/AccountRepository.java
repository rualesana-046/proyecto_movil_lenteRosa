package com.lenterosa.api.users.application;

import com.lenterosa.api.users.domain.Account;
import java.time.Instant;
import java.util.Optional;

/** Puerto de persistencia de cuentas. Lo implementa la capa de infraestructura. */
public interface AccountRepository {

    Optional<Account> findByUid(String uid);

    /** Registra la cuenta si no existe (seguro ante registros simultáneos) y devuelve la guardada. */
    Account registerIfAbsent(String uid, Instant registeredAt);

    /** Borra la cuenta; devuelve si existía. */
    boolean delete(String uid);
}
