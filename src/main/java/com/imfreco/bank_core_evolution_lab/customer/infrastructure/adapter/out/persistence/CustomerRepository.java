package com.imfreco.bank_core_evolution_lab.customer.infrastructure.adapter.out.persistence;

import com.imfreco.bank_core_evolution_lab.customer.application.port.out.CustomerRepositoryPort;
import com.imfreco.bank_core_evolution_lab.customer.domain.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, CustomerRepositoryPort {

    Optional<Customer> findByDocumentTypeAndDocumentNumber(
            String documentType, String documentNumber);
}
