package vn.io.sanmaymac.modules.message.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.io.sanmaymac.modules.message.entity.WorkshopMessageThreadEntity;

public interface WorkshopMessageThreadRepository extends JpaRepository<WorkshopMessageThreadEntity, Long> {
    Optional<WorkshopMessageThreadEntity> findByOrderId(Long orderId);

    @Query("select t from WorkshopMessageThreadEntity t where t.customer.id = :userId or t.workshop.id = :userId order by t.lastMessageAt desc nulls last, t.createdAt desc")
    List<WorkshopMessageThreadEntity> findMyThreads(@Param("userId") Long userId);

    @Query("select t from WorkshopMessageThreadEntity t where t.id = :threadId and (t.customer.id = :userId or t.workshop.id = :userId)")
    Optional<WorkshopMessageThreadEntity> findByIdAndParticipant(
            @Param("threadId") Long threadId,
            @Param("userId") Long userId);
}