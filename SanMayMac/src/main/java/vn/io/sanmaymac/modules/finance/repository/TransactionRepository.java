package vn.io.sanmaymac.modules.finance.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.common.enums.TransactionStatus;
import vn.io.sanmaymac.common.enums.TransactionType;
import vn.io.sanmaymac.modules.finance.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findByUserId(Long userId);

    List<TransactionEntity> findByUserIdAndType(Long userId, TransactionType type);

    List<TransactionEntity> findByUserIdAndStatus(Long userId, TransactionStatus status);

    List<TransactionEntity> findByCreatedAtBetween(Instant from, Instant to);

    List<TransactionEntity> findByOrderIdAndTypeAndStatus(Long orderId, TransactionType type, TransactionStatus status);

    Optional<TransactionEntity> findByTransactionCode(String transactionCode);

    List<TransactionEntity> findByCheckoutBatchIdAndTypeAndStatus(
            String checkoutBatchId,
            TransactionType type,
            TransactionStatus status);
}
