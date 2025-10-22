package org.ravo.client.repository;

import org.ravo.client.domain.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

    boolean existsByRequestId(String requestId);
}
