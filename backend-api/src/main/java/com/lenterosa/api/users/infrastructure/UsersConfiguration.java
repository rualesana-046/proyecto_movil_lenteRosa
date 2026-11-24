package com.lenterosa.api.users.infrastructure;

import com.lenterosa.api.reviews.ReviewAuthorship;
import com.lenterosa.api.users.application.AccountRepository;
import com.lenterosa.api.users.application.AccountService;
import com.lenterosa.api.users.application.SavedPlaceRepository;
import com.lenterosa.api.users.application.SavedPlaceService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de cuentas y de lugares guardados. */
@Configuration(proxyBeanMethods = false)
class UsersConfiguration {

    @Bean
    AccountService accountService(AccountRepository accountRepository, ReviewAuthorship reviewAuthorship, Clock clock) {
        return new AccountService(accountRepository, reviewAuthorship, clock);
    }

    @Bean
    SavedPlaceService savedPlaceService(SavedPlaceRepository savedPlaceRepository, AccountRepository accountRepository, Clock clock) {
        return new SavedPlaceService(savedPlaceRepository, accountRepository, clock);
    }
}
