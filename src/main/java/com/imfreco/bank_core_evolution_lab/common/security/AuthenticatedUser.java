package com.imfreco.bank_core_evolution_lab.common.security;

import java.util.List;

public record AuthenticatedUser(String username, List<String> roles) {}
