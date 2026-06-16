package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.approval.ApprovalComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalCommentRepository extends JpaRepository<ApprovalComment, UUID> {

	List<ApprovalComment> findByApprovalRequestId(UUID approvalRequestId);
}
