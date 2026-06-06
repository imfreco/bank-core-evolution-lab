package com.imfreco.bank_core_evolution_lab.customer.infrastructure;

import com.imfreco.bank_core_evolution_lab.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);
}
