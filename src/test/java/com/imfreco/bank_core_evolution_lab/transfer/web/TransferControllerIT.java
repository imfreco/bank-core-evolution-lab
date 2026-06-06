package com.imfreco.bank_core_evolution_lab.transfer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEventStatus;
import com.imfreco.bank_core_evolution_lab.outbox.infrastructure.OutboxEventRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransferControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bank_core")
                    .withUsername("bank")
                    .withPassword("bank");

    @Container static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("bank.outbox.publisher.enabled", () -> "false");
        registry.add("bank.projections.mongodb.enabled", () -> "false");
    }

    @Autowired MockMvc mockMvc;

    @Autowired ObjectMapper objectMapper;

    @Autowired OutboxEventRepository outboxEventRepository;

    @Test
    void createsSuccessfulTransferMovementsAndOutboxEvent() throws Exception {
        String response =
                postTransfer(UUID.randomUUID().toString(), "10000.00")
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
                        .andExpect(jsonPath("$.sourceAccountNumber").value("1000000001"))
                        .andExpect(jsonPath("$.targetAccountNumber").value("1000000002"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        JsonNode json = objectMapper.readTree(response);

        mockMvc.perform(
                        get("/api/v1/accounts/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/movements")
                                .with(httpBasic("customer", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(
                        jsonPath("$[0].transferReference")
                                .value(json.get("transferReference").asText()))
                .andExpect(jsonPath("$[0].type").value("DEBIT"));

        assertThat(outboxEventRepository.countByStatus(OutboxEventStatus.PENDING)).isEqualTo(1);
    }

    @Test
    void rejectsTransferWithInsufficientFunds() throws Exception {
        postTransfer(UUID.randomUUID().toString(), "999999999.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void rejectsTransferFromBlockedAccount() throws Exception {
        String body =
                """
                {
                  "sourceAccountNumber": "1000000003",
                  "targetAccountNumber": "1000000002",
                  "amount": 1000.00,
                  "currency": "COP"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/transfers")
                                .with(httpBasic("customer", "customer123"))
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .header("X-Correlation-ID", "it-correlation")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("ACCOUNT_BLOCKED"));
    }

    @Test
    void sameIdempotencyKeyAndSameBodyReturnsSameTransfer() throws Exception {
        String key = UUID.randomUUID().toString();
        String first =
                postTransfer(key, "15000.00")
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String second =
                postTransfer(key, "15000.00")
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(objectMapper.readTree(first).get("transferReference").asText())
                .isEqualTo(objectMapper.readTree(second).get("transferReference").asText());
    }

    @Test
    void sameIdempotencyKeyWithDifferentBodyReturnsConflict() throws Exception {
        String key = UUID.randomUUID().toString();
        postTransfer(key, "1000.00").andExpect(status().isCreated());
        postTransfer(key, "2000.00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_IDEMPOTENCY_KEY"));
    }

    private org.springframework.test.web.servlet.ResultActions postTransfer(
            String idempotencyKey, String amount) throws Exception {
        String body =
                """
                {
                  "sourceAccountNumber": "1000000001",
                  "targetAccountNumber": "1000000002",
                  "amount": %s,
                  "currency": "COP"
                }
                """
                        .formatted(amount);

        return mockMvc.perform(
                post("/api/v1/transfers")
                        .with(httpBasic("customer", "customer123"))
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", "it-correlation")
                        .contentType("application/json")
                        .content(body));
    }
}
