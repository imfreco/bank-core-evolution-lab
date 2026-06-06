package com.imfreco.bank_core_evolution_lab.common.api;

import java.security.Principal;

public final class ActorContext {

    private ActorContext() {}

    public static String actor(Principal principal) {
        return principal == null ? "anonymous" : principal.getName();
    }

    public static String channel(String channel) {
        return channel == null || channel.isBlank() ? "WEB" : channel.trim().toUpperCase();
    }
}
