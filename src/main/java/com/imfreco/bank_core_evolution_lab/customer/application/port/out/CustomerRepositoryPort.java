package com.imfreco.bank_core_evolution_lab.customer.application.port.out;

import com.imfreco.bank_core_evolution_lab.customer.domain.Customer;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID customerId);

    boolean existsById(UUID customerId);

    Optional<Customer> findByDocumentTypeAndDocumentNumber(
            String documentType, String documentNumber);
}
