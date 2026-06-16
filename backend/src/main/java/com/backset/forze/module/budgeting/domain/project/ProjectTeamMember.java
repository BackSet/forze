package com.backset.forze.module.budgeting.domain.project;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Project team membership (design section 6.2 "Equipo del proyecto"). Members are referenced by
 * user id without a cross-module foreign key to identity.
 */
@Entity
@Table(name = "budgeting_project_team")
public class ProjectTeamMember {

	@EmbeddedId
	private ProjectTeamMemberId id;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ProjectTeamMember() {
	}

	public ProjectTeamMember(UUID projectId, UUID userId) {
		this.id = new ProjectTeamMemberId(projectId, userId);
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public UUID projectId() {
		return id.projectId();
	}

	public UUID userId() {
		return id.userId();
	}

	@Embeddable
	public static class ProjectTeamMemberId implements Serializable {

		@Column(name = "project_id", nullable = false)
		private UUID projectId;

		@Column(name = "user_id", nullable = false)
		private UUID userId;

		protected ProjectTeamMemberId() {
		}

		public ProjectTeamMemberId(UUID projectId, UUID userId) {
			this.projectId = projectId;
			this.userId = userId;
		}

		public UUID projectId() {
			return projectId;
		}

		public UUID userId() {
			return userId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ProjectTeamMemberId other)) {
				return false;
			}
			return Objects.equals(projectId, other.projectId) && Objects.equals(userId, other.userId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(projectId, userId);
		}
	}
}
