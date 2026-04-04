package com.vocaldocs.repositories;

import com.vocaldocs.models.History;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {
    List<History> findByUserIdOrderByTimestampDesc(Long userId);
}
