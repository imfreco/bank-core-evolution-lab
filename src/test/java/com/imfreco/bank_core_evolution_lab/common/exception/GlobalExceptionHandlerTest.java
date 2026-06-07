package com.imfreco.bank_core_evolution_lab.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.imfreco.bank_core_evolution_lab.common.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void mapsAccessDeniedToForbiddenInsteadOfInternalServerError() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/accounts/account-id");
        MDC.put(CorrelationIdFilter.MDC_KEY, "corr-403");

        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("Denied"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody())
                .satisfies(
                        body -> {
                            assertThat(body).isNotNull();
                            assertThat(body.error()).isEqualTo("FORBIDDEN");
                            assertThat(body.correlationId()).isEqualTo("corr-403");
                        });
    }
}
