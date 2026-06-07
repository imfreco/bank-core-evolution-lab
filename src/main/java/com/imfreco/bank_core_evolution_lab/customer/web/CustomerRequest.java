package com.imfreco.bank_core_evolution_lab.customer.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank String documentType,
        @NotBlank String documentNumber,
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @NotBlank String username,
        @NotBlank String password) {}
