package site.kkokkio.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.enums.ReportProcessingStatus;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.util.BaseTimeEntity;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostReport extends BaseTimeEntity {

	// 신고 ID
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "post_report_id")
	private Long id;

	// 신고된 포스트와의 관계
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	// 신고한 사용자와의 관계
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reporter_id", nullable = false)
	private Member reporter;

	// 신고 사유
	@Enumerated(EnumType.STRING)
	@Column(name = "reason", nullable = false)
	@Builder.Default
	private ReportReason reason = ReportReason.BAD_CONTENT;

	// 기타 사유
	@Column(name = "etc_reason", columnDefinition = "TEXT", nullable = true)
	private String etcReason;

	// 관리자 처리 상태 필드
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	@Builder.Default
	private ReportProcessingStatus status = ReportProcessingStatus.PENDING;
}
