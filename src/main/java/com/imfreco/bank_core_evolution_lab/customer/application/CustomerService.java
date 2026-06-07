package com.imfreco.bank_core_evolution_lab.customer.application;

import com.imfreco.bank_core_evolution_lab.account.infrastructure.AccountRepository;
import com.imfreco.bank_core_evolution_lab.account.web.AccountMapper;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.exception.DuplicateUsernameException;
import com.imfreco.bank_core_evolution_lab.common.security.AuthUser;
import com.imfreco.bank_core_evolution_lab.common.security.AuthUserRepository;
import com.imfreco.bank_core_evolution_lab.customer.domain.Customer;
import com.imfreco.bank_core_evolution_lab.customer.infrastructure.CustomerRepository;
import com.imfreco.bank_core_evolution_lab.customer.web.CustomerMapper;
import com.imfreco.bank_core_evolution_lab.customer.web.CustomerProductsResponse;
import com.imfreco.bank_core_evolution_lab.customer.web.CustomerRequest;
import com.imfreco.bank_core_evolution_lab.customer.web.CustomerResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        authUserRepository
                .findByUsername(request.username())
                .ifPresent(
                        existing -> {
                            throw new DuplicateUsernameException(request.username());
                        });
        Customer customer =
                new Customer(
                        request.documentType(),
                        request.documentNumber(),
                        request.fullName(),
                        request.email());
        Customer savedCustomer = customerRepository.save(customer);
        authUserRepository.save(
                new AuthUser(
                        request.username(),
                        passwordEncoder.encode(request.password()),
                        savedCustomer.getId(),
                        true,
                        List.of("CUSTOMER")));
        return CustomerMapper.toResponse(savedCustomer);
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
