package site.kkokkio.domain.member.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.util.BaseTimeEntity;

@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(name = "member_id")
	private UUID id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(nullable = false, unique = true)
	private String nickname;

	@Column(name = "birth_date", nullable = false)
	private LocalDate birthDate;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberRole role = MemberRole.USER;

	@Builder.Default
	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified = false;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	// 역할 변경을 위한 Setter
	public void setRole(MemberRole role) {
		this.role = role;
	}

	public void maskPersonalInfo() {
		String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

		this.email = "deleted_" + this.id + "_" + uuid + "@deleted.com";
		this.nickname = "탈퇴한사용자_" + this.id + "_" + uuid;
	}
}
