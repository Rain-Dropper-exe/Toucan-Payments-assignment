package com.example.transactionstarter.service;

import com.example.transactionstarter.dto.CreateTransactionRequest;
import com.example.transactionstarter.dto.TransactionResponse;

public interface TransactionService {
    TransactionResponse createTransaction(CreateTransactionRequest request);
}
