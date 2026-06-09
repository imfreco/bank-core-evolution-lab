package com.imfreco.bank_core_evolution_lab.customer.application.port.in;

import java.util.UUID;

public interface CustomerUseCase {

    CustomerResult create(CreateCustomerCommand command);

    CustomerResult get(UUID customerId);

    CustomerProductsResult getProducts(UUID customerId);
}
