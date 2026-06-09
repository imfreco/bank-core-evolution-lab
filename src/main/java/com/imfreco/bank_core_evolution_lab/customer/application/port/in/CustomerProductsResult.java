package com.imfreco.bank_core_evolution_lab.customer.application.port.in;

import com.imfreco.bank_core_evolution_lab.account.application.port.in.AccountResult;
import java.util.List;
import java.util.UUID;

public record CustomerProductsResult(UUID customerId, List<AccountResult> accounts) {}
