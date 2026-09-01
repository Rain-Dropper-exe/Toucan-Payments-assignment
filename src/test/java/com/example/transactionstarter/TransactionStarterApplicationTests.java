package com.example.transactionstarter;

import com.example.transactionstarter.dto.CreateTransactionRequest;
import com.example.transactionstarter.dto.UpdateStatusRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    // 2. API 2: Get Transaction by ID Success -> Return 200 OK
    @Test
    void testGetTransactionSuccess() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-FETCH-1",
                "CUST-2002",
                new BigDecimal("500.00"),
                Currency.EUR,
                TransactionType.PAYMENT
        );
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions/TXN-FETCH-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN-FETCH-1"))
                .andExpect(jsonPath("$.customerId").value("CUST-2002"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.type").value("PAYMENT"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // 3. API 3: Update Transaction Status Success -> Return 200 OK
    @Test
    void testUpdateTransactionStatusSuccess() throws Exception {
        CreateTransactionRequest createRequest = new CreateTransactionRequest(
                "TXN-STATUS-1",
                "CUST-2002",
                new BigDecimal("100.00"),
                Currency.USD,
                TransactionType.PAYMENT
        );
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        UpdateStatusRequest updateRequest = new UpdateStatusRequest(TransactionStatus.COMPLETED);

        mockMvc.perform(patch("/api/transactions/TXN-STATUS-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN-STATUS-1"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // 4. API 4: Get Customer Transactions Success -> Return 200 OK with list
    @Test
    void testGetCustomerTransactionsSuccess() throws Exception {
        // Create 2 transactions for CUST-A
        CreateTransactionRequest reqA1 = new CreateTransactionRequest("TXN-A1", "CUST-A", new BigDecimal("100.00"), Currency.USD, TransactionType.PAYMENT);
        CreateTransactionRequest reqA2 = new CreateTransactionRequest("TXN-A2", "CUST-A", new BigDecimal("200.00"), Currency.EUR, TransactionType.REFUND);
        // Create 1 transaction for CUST-B
        CreateTransactionRequest reqB1 = new CreateTransactionRequest("TXN-B1", "CUST-B", new BigDecimal("300.00"), Currency.GBP, TransactionType.TRANSFER);

        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reqA1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reqA2))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(reqB1))).andExpect(status().isCreated());

        // Fetch transactions for CUST-A only
        mockMvc.perform(get("/api/customers/CUST-A/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].transactionId").value("TXN-A1"))
                .andExpect(jsonPath("$[0].customerId").value("CUST-A"))
                .andExpect(jsonPath("$[1].transactionId").value("TXN-A2"))
                .andExpect(jsonPath("$[1].customerId").value("CUST-A"));
    }

    // --- Validation, Error Handling, and Business Rule Tests ---

    // 5. Validation Failure: Negative Amount -> Return 400 Bad Request
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

    // 6. Business Rule Failure: Duplicate Transaction ID -> Return 409 Conflict
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

    // 7. Business Rule Failure: Amount Scale Exceeds 2 Decimal Places -> Return 400 Bad Request
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
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // 8. Error Handling: Transaction Not Found -> Return 404 Not Found
    @Test
    void testGetTransactionNotFound() throws Exception {
        mockMvc.perform(get("/api/transactions/NON-EXISTENT-TXN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transaction not found with ID: NON-EXISTENT-TXN"));
    }

    // 9. State Machine Failure: Disallowed Transition From Terminal State -> Return 400 Bad Request
    @Test
    void testUpdateTransactionStatusInvalidTransition() throws Exception {
        CreateTransactionRequest createRequest = new CreateTransactionRequest(
                "TXN-STATUS-INVALID",
                "CUST-2002",
                new BigDecimal("100.00"),
                Currency.USD,
                TransactionType.PAYMENT
        );
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        UpdateStatusRequest failRequest = new UpdateStatusRequest(TransactionStatus.FAILED);
        mockMvc.perform(patch("/api/transactions/TXN-STATUS-INVALID/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failRequest)))
                .andExpect(status().isOk());

        UpdateStatusRequest completeRequest = new UpdateStatusRequest(TransactionStatus.COMPLETED);
        mockMvc.perform(patch("/api/transactions/TXN-STATUS-INVALID/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Status Transition"));
    }

    // 10. State Machine Failure: Same-Status Update Rejected -> Return 400 Bad Request
    @Test
    void testUpdateTransactionStatusSameStatusRejected() throws Exception {
        CreateTransactionRequest createRequest = new CreateTransactionRequest(
                "TXN-STATUS-SAME",
                "CUST-2002",
                new BigDecimal("100.00"),
                Currency.USD,
                TransactionType.PAYMENT
        );
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        UpdateStatusRequest sameStatusRequest = new UpdateStatusRequest(TransactionStatus.PENDING);
        mockMvc.perform(patch("/api/transactions/TXN-STATUS-SAME/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sameStatusRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Status Transition"));
    }

    // 11. State Machine Failure: Disallowed Direct Jump (PENDING -> REFUNDED) -> Return 400 Bad Request
    @Test
    void testUpdateTransactionStatusPendingToRefundedRejected() throws Exception {
        CreateTransactionRequest createRequest = new CreateTransactionRequest(
                "TXN-STATUS-JUMP",
                "CUST-2002",
                new BigDecimal("100.00"),
                Currency.USD,
                TransactionType.PAYMENT
        );
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Attempt direct jump: PENDING -> REFUNDED (Must be rejected, can only refund from COMPLETED)
        UpdateStatusRequest refundRequest = new UpdateStatusRequest(TransactionStatus.REFUNDED);
        mockMvc.perform(patch("/api/transactions/TXN-STATUS-JUMP/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Status Transition"));
    }

    // 12. Error Handling: Update Status on Non-Existent Transaction -> Return 404 Not Found
    @Test
    void testUpdateTransactionStatusNotFound() throws Exception {
        UpdateStatusRequest updateRequest = new UpdateStatusRequest(TransactionStatus.COMPLETED);
        mockMvc.perform(patch("/api/transactions/NON-EXISTENT-TXN/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transaction not found with ID: NON-EXISTENT-TXN"));
    }

    // 13. Customer Lookup: Non-Existent or Customer With No Transactions -> Return 200 OK with empty array []
    @Test
    void testGetCustomerTransactionsEmptyList() throws Exception {
        mockMvc.perform(get("/api/customers/NON-EXISTENT-CUST/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
