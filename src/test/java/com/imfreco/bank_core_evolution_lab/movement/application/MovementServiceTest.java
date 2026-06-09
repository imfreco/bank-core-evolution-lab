package com.imfreco.bank_core_evolution_lab.movement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.imfreco.bank_core_evolution_lab.account.application.port.out.AccountRepositoryPort;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.customer.application.port.out.CustomerRepositoryPort;
import com.imfreco.bank_core_evolution_lab.movement.application.port.in.CustomerMovementViewResult;
import com.imfreco.bank_core_evolution_lab.movement.application.port.out.MovementProjectionPort;
import com.imfreco.bank_core_evolution_lab.movement.application.port.out.MovementRepositoryPort;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.out.TransferRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MovementServiceTest {

    private final MovementRepositoryPort movementRepository = mock(MovementRepositoryPort.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final CustomerRepositoryPort customerRepository = mock(CustomerRepositoryPort.class);
    private final TransferRepositoryPort transferRepository = mock(TransferRepositoryPort.class);
    private final MovementProjectionPort projectionService = mock(MovementProjectionPort.class);
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
        CustomerMovementViewResult mongoResponse =
                new CustomerMovementViewResult(
                        customerId, List.of(), Instant.now(), "MONGODB_READ_MODEL");
        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(projectionService.findView(customerId)).thenReturn(Optional.of(mongoResponse));

        CustomerMovementViewResult response = service.findByCustomer(customerId);

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
