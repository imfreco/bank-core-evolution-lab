package com.imfreco.bank_core_evolution_lab.customer.web;

import com.imfreco.bank_core_evolution_lab.customer.application.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CUSTOMER')")
    public CustomerResponse get(@PathVariable UUID customerId) {
        return customerService.get(customerId);
    }

    @GetMapping("/{customerId}/products")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CUSTOMER')")
    public CustomerProductsResponse products(@PathVariable UUID customerId) {
        return customerService.getProducts(customerId);
    }
}
