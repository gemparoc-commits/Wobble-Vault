package com.wobblevault.backend.features.income;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.wobblevault.backend.entity.IncomeSource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncomeSourceRepository extends JpaRepository<IncomeSource, UUID> {
    List<IncomeSource> findByIncomeDate(LocalDate incomeDate);

    List<IncomeSource> findByIncomeDateBetween(LocalDate startDate, LocalDate endDate);

    List<IncomeSource> findByJobOrderNo(String jobOrderNo);

    List<IncomeSource> findByJobOrderNoOrderByCreatedAtAsc(String jobOrderNo);
}
