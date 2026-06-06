package com.imfreco.bank_core_evolution_lab.account.infrastructure;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(UUID customerId);

    /*
     * Transferencias bancarias concurrentes suelen usar bloqueo pesimista sobre las
     * filas que cambian saldo. El orden estable evita deadlocks en transferencias
     * cruzadas A->B y B->A. @Version queda como segunda línea defensiva para otros
     * flujos de actualización más optimistas y de menor contención.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber in :accountNumbers order by a.accountNumber asc")
    List<Account> findAllByAccountNumberInForUpdate(@Param("accountNumbers") Collection<String> accountNumbers);
}
