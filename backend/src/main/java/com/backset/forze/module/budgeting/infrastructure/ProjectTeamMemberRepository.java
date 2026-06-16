package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.project.ProjectTeamMember;
import com.backset.forze.module.budgeting.domain.project.ProjectTeamMember.ProjectTeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTeamMemberRepository extends JpaRepository<ProjectTeamMember, ProjectTeamMemberId> {

	List<ProjectTeamMember> findByIdProjectId(UUID projectId);
}
