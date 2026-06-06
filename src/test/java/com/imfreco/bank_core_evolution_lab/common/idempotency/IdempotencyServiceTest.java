package com.imfreco.bank_core_evolution_lab.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imfreco.bank_core_evolution_lab.common.exception.DuplicateIdempotencyKeyException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyServiceTest {

    private final IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
    private final IdempotencyService service = new IdempotencyService(repository, new ObjectMapper());

    @Test
    void createsRecordWhenKeyIsNew() {
        SampleRequest request = new SampleRequest("A", 100);

        Optional<SampleResponse> response = service.findCompletedResponseOrCreateRecord(
                "idem-1", request, "TEST", SampleResponse.class);

        assertThat(response).isEmpty();
        verify(repository).saveAndFlush(any(IdempotencyRecord.class));
    }

    @Test
    void returnsStoredResponseForSameKeyAndSameBody() {
        SampleRequest request = new SampleRequest("A", 100);
        IdempotencyRecord record = new IdempotencyRecord("idem-1", service.hashRequest(request), "TEST");
        record.complete("{\"reference\":\"tx-1\"}");
        when(repository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(record));

        Optional<SampleResponse> response = service.findCompletedResponseOrCreateRecord(
                "idem-1", request, "TEST", SampleResponse.class);

        assertThat(response).isPresent();
        assertThat(response.get().reference()).isEqualTo("tx-1");
    }

    @Test
    void rejectsSameKeyWithDifferentBody() {
        IdempotencyRecord record = new IdempotencyRecord(
                "idem-1",
                service.hashRequest(new SampleRequest("A", 100)),
                "TEST");
        when(repository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.findCompletedResponseOrCreateRecord(
                "idem-1", new SampleRequest("A", 200), "TEST", SampleResponse.class))
                .isInstanceOf(DuplicateIdempotencyKeyException.class);
    }

    record SampleRequest(String account, int amount) {
    }

    record SampleResponse(String reference) {
    }
}
