package com.imfreco.bank_core_evolution_lab.account.application.port.out;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepositoryPort {

    Account save(Account account);

    Optional<Account> findById(UUID accountId);

    boolean existsById(UUID accountId);

    List<Account> findAll();

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(UUID customerId);

    List<Account> findAllByAccountNumberInForUpdate(Collection<String> accountNumbers);
}
