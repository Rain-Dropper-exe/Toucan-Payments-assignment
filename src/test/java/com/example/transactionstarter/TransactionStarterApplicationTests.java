package com.example.transactionstarter;

import com.example.transactionstarter.dto.CreateTransactionRequest;
import com.example.transactionstarter.enums.Currency;
import com.example.transactionstarter.enums.TransactionStatus;
import com.example.transactionstarter.enums.TransactionType;
import com.example.transactionstarter.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionStarterApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    // --- Primary API Integration Tests ---

    // 1. API 1: Create Transaction Success -> Return 201 Created
    @Test
    void testCreateTransactionSuccess() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-1001",
                "CUST-2002",
                new BigDecimal("150.75"),
                Currency.USD,
                TransactionType.PAYMENT
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-1001"))
                .andExpect(jsonPath("$.customerId").value("CUST-2002"))
                .andExpect(jsonPath("$.amount").value(150.75))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.type").value("PAYMENT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()));
    }

    // --- Validation, Error Handling, and Business Rule Tests ---

    // 2. Validation Failure: Negative Amount -> Return 400 Bad Request
    @Test
    void testCreateTransactionValidationFailure() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-1002",
                "CUST-2002",
                new BigDecimal("-50.00"),
                Currency.USD,
                TransactionType.PAYMENT
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // 3. Business Rule Failure: Duplicate Transaction ID -> Return 409 Conflict
    @Test
    void testCreateTransactionDuplicateId() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-DUPLICATE",
                "CUST-2002",
                new BigDecimal("100.00"),
                Currency.USD,
                TransactionType.PAYMENT
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    // 4. Business Rule Failure: Amount Scale Exceeds 2 Decimal Places -> Return 400 Bad Request
    @Test
    void testCreateTransactionAmountScaleFailure() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-1003",
                "CUST-2002",
                new BigDecimal("10.123"),
                Currency.USD,
                TransactionType.PAYMENT
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
