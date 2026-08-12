package com.youjian.banquet.repository;

import com.youjian.banquet.entity.Reimbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReimbursementRepository extends JpaRepository<Reimbursement, Long> {

    List<Reimbursement> findByStoreId(Long storeId);

    List<Reimbursement> findByStoreIdAndStatus(Long storeId, String status);

    List<Reimbursement> findByApplicantId(Integer applicantId);

    Optional<Reimbursement> findByReimbursementNo(String reimbursementNo);
}
