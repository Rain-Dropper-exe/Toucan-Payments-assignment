package com.example.transactionstarter.service;

import com.example.transactionstarter.dto.CreateTransactionRequest;
import com.example.transactionstarter.dto.TransactionResponse;
import com.example.transactionstarter.enums.TransactionStatus;
import com.example.transactionstarter.exception.DuplicateTransactionException;
import com.example.transactionstarter.exception.InvalidTransactionException;
import com.example.transactionstarter.model.Transaction;
import com.example.transactionstarter.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        // 1. Business Validation: Reject duplicate Transaction ID
        if (transactionRepository.existsById(request.getTransactionId())) {
            throw new DuplicateTransactionException("Transaction with ID " + request.getTransactionId() + " already exists");
        }

        // 2. Business Validation: Reject amount with more than 2 decimal places
        BigDecimal amount = request.getAmount();
        if (amount.scale() > 2) {
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
}
