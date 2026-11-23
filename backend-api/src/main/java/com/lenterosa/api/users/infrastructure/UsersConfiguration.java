package com.lenterosa.api.users.infrastructure;

import com.lenterosa.api.reviews.ReviewAuthorship;
import com.lenterosa.api.users.application.AccountRepository;
import com.lenterosa.api.users.application.AccountService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de cuentas. */
@Configuration(proxyBeanMethods = false)
class UsersConfiguration {

    @Bean
    AccountService accountService(AccountRepository accountRepository, ReviewAuthorship reviewAuthorship, Clock clock) {
        return new AccountService(accountRepository, reviewAuthorship, clock);
    }
}
