package com.imfreco.bank_core_evolution_lab.customer.application;

import com.imfreco.bank_core_evolution_lab.account.application.port.in.AccountResult;
import com.imfreco.bank_core_evolution_lab.account.application.port.out.AccountRepositoryPort;
import com.imfreco.bank_core_evolution_lab.common.application.port.out.CustomerAuthenticationPort;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.exception.DuplicateUsernameException;
import com.imfreco.bank_core_evolution_lab.customer.application.port.in.CreateCustomerCommand;
import com.imfreco.bank_core_evolution_lab.customer.application.port.in.CustomerProductsResult;
import com.imfreco.bank_core_evolution_lab.customer.application.port.in.CustomerResult;
import com.imfreco.bank_core_evolution_lab.customer.application.port.in.CustomerUseCase;
import com.imfreco.bank_core_evolution_lab.customer.application.port.out.CustomerRepositoryPort;
import com.imfreco.bank_core_evolution_lab.customer.domain.Customer;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService implements CustomerUseCase {

    private final CustomerRepositoryPort customerRepository;
    private final AccountRepositoryPort accountRepository;
    private final CustomerAuthenticationPort customerAuthentication;

    public CustomerService(
            CustomerRepositoryPort customerRepository,
            AccountRepositoryPort accountRepository,
            CustomerAuthenticationPort customerAuthentication) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.customerAuthentication = customerAuthentication;
    }

    @Override
    @Transactional
    public CustomerResult create(CreateCustomerCommand command) {
        if (customerAuthentication.usernameExists(command.username())) {
            throw new DuplicateUsernameException(command.username());
        }
        Customer customer =
                new Customer(
                        command.documentType(),
                        command.documentNumber(),
                        command.fullName(),
                        command.email());
        Customer savedCustomer = customerRepository.save(customer);
        customerAuthentication.registerCustomerCredentials(
                command.username(), command.password(), savedCustomer.getId());
        return toResult(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResult get(UUID customerId) {
        return toResult(findCustomer(customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProductsResult getProducts(UUID customerId) {
        findCustomer(customerId);
        return new CustomerProductsResult(
                customerId,
                accountRepository.findByCustomerId(customerId).stream()
                        .map(
                                account ->
                                        new AccountResult(
                                                account.getId(),
                                                account.getAccountNumber(),
                                                account.getCustomerId(),
                                                account.getType(),
                                                account.getStatus(),
                                                account.getCurrency(),
                                                account.getAccountingBalance(),
                                                account.getAvailableBalance(),
                                                account.getVersion(),
                                                account.getCreatedAt(),
                                                account.getUpdatedAt()))
                        .toList());
    }

    public Customer findCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private CustomerResult toResult(Customer customer) {
        return new CustomerResult(
                customer.getId(),
                customer.getDocumentType(),
                customer.getDocumentNumber(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
