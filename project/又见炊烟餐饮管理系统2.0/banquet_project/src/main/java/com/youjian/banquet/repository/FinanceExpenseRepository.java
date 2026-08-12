package com.youjian.banquet.repository;

import com.youjian.banquet.entity.FinanceExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinanceExpenseRepository extends JpaRepository<FinanceExpense, Long> {

    List<FinanceExpense> findByStoreIdOrderByExpenseIdDesc(Long storeId);

    List<FinanceExpense> findByStoreIdAndExpenseNoContainingOrderByExpenseIdDesc(Long storeId, String expenseNo);

    List<FinanceExpense> findByStoreIdAndApplicantNameContainingOrderByExpenseIdDesc(Long storeId, String applicantName);

    List<FinanceExpense> findByStoreIdAndExpenseTypeOrderByExpenseIdDesc(Long storeId, String expenseType);

    List<FinanceExpense> findByStoreIdAndStatusOrderByExpenseIdDesc(Long storeId, String status);

    List<FinanceExpense> findByStoreIdAndExpenseNoContainingAndExpenseTypeAndStatusOrderByExpenseIdDesc(
            Long storeId, String expenseNo, String expenseType, String status);
}
