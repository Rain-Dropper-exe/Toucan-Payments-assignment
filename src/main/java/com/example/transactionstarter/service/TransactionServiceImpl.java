package com.example.transactionstarter.service;

import com.example.transactionstarter.dto.CreateTransactionRequest;
import com.example.transactionstarter.dto.TransactionResponse;
import com.example.transactionstarter.dto.UpdateStatusRequest;
import com.example.transactionstarter.enums.TransactionStatus;
import com.example.transactionstarter.exception.DuplicateTransactionException;
import com.example.transactionstarter.exception.InvalidTransactionException;
import com.example.transactionstarter.exception.TransactionNotFoundException;
import com.example.transactionstarter.model.Transaction;
import com.example.transactionstarter.repository.TransactionRepository;
import com.example.transactionstarter.validation.StatusTransitionValidator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final StatusTransitionValidator statusTransitionValidator;

    public TransactionServiceImpl(TransactionRepository transactionRepository, StatusTransitionValidator statusTransitionValidator) {
        this.transactionRepository = transactionRepository;
        this.statusTransitionValidator = statusTransitionValidator;
    }

    @Override
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        // 1. Business Validation: Reject duplicate Transaction ID
        if (transactionRepository.existsById(request.getTransactionId())) {
            throw new DuplicateTransactionException("Transaction with ID " + request.getTransactionId() + " already exists");
        }

        // 2. Business Validation: Reject amount with more than 2 decimal places
        BigDecimal amount = request.getAmount();
        if (amount != null && amount.scale() > 2) {
            throw new InvalidTransactionException("Amount cannot have more than 2 decimal places");
        }

        // 3. Map Request DTO to JPA Database Entity
        Transaction transaction = new Transaction(
                request.getTransactionId(),
                request.getCustomerId(),
                request.getAmount(),
                request.getCurrency(),
                request.getType(),
                TransactionStatus.PENDING
        );

        // 4. Save the entity to H2 Database
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 5. Convert saved Entity back into a TransactionResponse DTO and return
        return new TransactionResponse(savedTransaction);
    }

    @Override
    public TransactionResponse getTransactionById(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));
        return new TransactionResponse(transaction);
    }

    @Override
    public TransactionResponse updateTransactionStatus(String transactionId, UpdateStatusRequest request) {
        // 1. Retrieve transaction by ID
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));

        // 2. Validate status transition rules
        statusTransitionValidator.validateTransition(transaction.getStatus(), request.getStatus());

        // 3. Update status and save
        transaction.setStatus(request.getStatus());
        Transaction updatedTransaction = transactionRepository.save(transaction);

        // 4. Return updated response DTO
        return new TransactionResponse(updatedTransaction);
    }

    @Override
    public List<TransactionResponse> getTransactionsByCustomerId(String customerId) {
        return transactionRepository.findByCustomerId(customerId)
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }
}
