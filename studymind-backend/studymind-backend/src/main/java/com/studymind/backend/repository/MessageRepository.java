package com.studymind.backend.repository;

import com.studymind.backend.model.Message;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByChatIdOrderByCreatedAtAsc(UUID chatId);
    void deleteByChatId(UUID chatId);

    @Query("select count(m) from Message m where m.kind = 'synthesis' and m.chatId in (select c.id from Chat c where c.userId = :userId)")
    long countAnswersForUser(@Param("userId") UUID userId);
}
