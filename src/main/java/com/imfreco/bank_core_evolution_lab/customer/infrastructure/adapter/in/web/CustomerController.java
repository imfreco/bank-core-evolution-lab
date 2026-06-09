package com.imfreco.bank_core_evolution_lab.customer.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.customer.application.port.in.CustomerUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerUseCase customerUseCase;

    public CustomerController(CustomerUseCase customerUseCase) {
        this.customerUseCase = customerUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return CustomerMapper.toResponse(customerUseCase.create(CustomerMapper.toCommand(request)));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("@authorizationService.canAccessCustomer(#customerId)")
    public CustomerResponse get(@PathVariable UUID customerId) {
        return CustomerMapper.toResponse(customerUseCase.get(customerId));
    }

    @GetMapping("/{customerId}/products")
    @PreAuthorize("@authorizationService.canAccessCustomer(#customerId)")
    public CustomerProductsResponse products(@PathVariable UUID customerId) {
        return CustomerMapper.toProductsResponse(customerUseCase.getProducts(customerId));
    }
}
