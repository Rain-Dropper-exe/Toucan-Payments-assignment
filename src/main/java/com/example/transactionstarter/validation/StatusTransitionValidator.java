package com.example.transactionstarter.validation;

import com.example.transactionstarter.enums.TransactionStatus;
import com.example.transactionstarter.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class StatusTransitionValidator {

    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS = Map.of(
            TransactionStatus.PENDING, Set.of(TransactionStatus.COMPLETED, TransactionStatus.FAILED),
            TransactionStatus.COMPLETED, Set.of(TransactionStatus.REFUNDED)
    );

    public void validateTransition(TransactionStatus currentStatus, TransactionStatus newStatus) {
        if (currentStatus == newStatus) {
            throw new InvalidStatusTransitionException(
                    "Transaction is already in status " + currentStatus + ". Same-status updates are not permitted."
            );
        }

        Set<TransactionStatus> validNextStatuses = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!validNextStatuses.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition transaction status from " + currentStatus + " to " + newStatus
            );
        }
    }
}
