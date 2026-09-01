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

    // --- Validation, Error Handling, and Business Rule Tests ---

    // 4. Validation Failure: Negative Amount -> Return 400 Bad Request
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

    // 5. Business Rule Failure: Duplicate Transaction ID -> Return 409 Conflict
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

    // 6. Business Rule Failure: Amount Scale Exceeds 2 Decimal Places -> Return 400 Bad Request
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

    // 7. Error Handling: Transaction Not Found -> Return 404 Not Found
    @Test
    void testGetTransactionNotFound() throws Exception {
        mockMvc.perform(get("/api/transactions/NON-EXISTENT-TXN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transaction not found with ID: NON-EXISTENT-TXN"));
    }

    // 8. State Machine Failure: Disallowed Transition From Terminal State -> Return 400 Bad Request
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

    // 9. State Machine Failure: Same-Status Update Rejected -> Return 400 Bad Request
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
}
