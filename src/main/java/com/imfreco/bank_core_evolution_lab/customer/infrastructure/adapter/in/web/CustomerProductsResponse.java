package com.imfreco.bank_core_evolution_lab.customer.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.account.infrastructure.adapter.in.web.AccountResponse;
import java.util.List;
import java.util.UUID;

public record CustomerProductsResponse(UUID customerId, List<AccountResponse> accounts) {}
