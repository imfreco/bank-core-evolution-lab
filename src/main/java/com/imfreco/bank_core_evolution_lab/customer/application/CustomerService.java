package com.imfreco.bank_core_evolution_lab.customer.application;

import com.imfreco.bank_core_evolution_lab.account.infrastructure.AccountRepository;
import com.imfreco.bank_core_evolution_lab.account.web.AccountMapper;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.customer.domain.Customer;
import com.imfreco.bank_core_evolution_lab.customer.infrastructure.CustomerRepository;
import com.imfreco.bank_core_evolution_lab.customer.web.CustomerMapper;
import com.imfreco.bank_core_evolution_lab.customer.web.CustomerProductsResponse;
import com.imfreco.bank_core_evolution_lab.customer.web.CustomerRequest;
import com.imfreco.bank_core_evolution_lab.customer.web.CustomerResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public CustomerService(
            CustomerRepository customerRepository, AccountRepository accountRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer =
                new Customer(
                        request.documentType(),
                        request.documentNumber(),
                        request.fullName(),
                        request.email());
        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID customerId) {
        return CustomerMapper.toResponse(findCustomer(customerId));
    }

    @Transactional(readOnly = true)
    public CustomerProductsResponse getProducts(UUID customerId) {
        findCustomer(customerId);
        return new CustomerProductsResponse(
                customerId,
                accountRepository.findByCustomerId(customerId).stream()
                        .map(AccountMapper::toResponse)
                        .toList());
    }

    public Customer findCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
