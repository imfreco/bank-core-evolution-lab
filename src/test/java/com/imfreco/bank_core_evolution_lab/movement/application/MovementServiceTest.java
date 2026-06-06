package com.imfreco.bank_core_evolution_lab.movement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.imfreco.bank_core_evolution_lab.account.infrastructure.AccountRepository;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.customer.infrastructure.CustomerRepository;
import com.imfreco.bank_core_evolution_lab.movement.infrastructure.MovementRepository;
import com.imfreco.bank_core_evolution_lab.movement.web.CustomerMovementViewResponse;
import com.imfreco.bank_core_evolution_lab.transfer.infrastructure.TransferRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MovementServiceTest {

    private final MovementRepository movementRepository = mock(MovementRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final TransferRepository transferRepository = mock(TransferRepository.class);
    private final MovementProjectionService projectionService =
            mock(MovementProjectionService.class);
    private final MovementService service =
            new MovementService(
                    movementRepository,
                    accountRepository,
                    customerRepository,
                    transferRepository,
                    projectionService);

    @Test
    void returnsMongoReadModelWhenAvailable() {
        UUID customerId = UUID.randomUUID();
        CustomerMovementViewResponse mongoResponse =
                new CustomerMovementViewResponse(
                        customerId, List.of(), Instant.now(), "MONGODB_READ_MODEL");
        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(projectionService.findView(customerId)).thenReturn(Optional.of(mongoResponse));

        CustomerMovementViewResponse response = service.findByCustomer(customerId);

        assertThat(response.source()).isEqualTo("MONGODB_READ_MODEL");
        verifyNoInteractions(movementRepository);
    }

    @Test
    void rejectsUnknownCustomer() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(false);

        assertThatThrownBy(() -> service.findByCustomer(customerId))
                .isInstanceOf(CustomerNotFoundException.class);
    }
}
