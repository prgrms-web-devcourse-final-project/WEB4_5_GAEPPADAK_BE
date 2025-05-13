package site.kkokkio.domain.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.enums.ReportProcessingStatus;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.util.BaseTimeEntity;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "comment_report",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"comment_id", "reporter_id"})
        })
public class CommentReport extends BaseTimeEntity {

    // 신고 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_report_id")
    private Long id;

    // 신고된 댓글과의 관계
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    // 신고한 사용자와의 관계
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    // 신고 사유
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    @Builder.Default
    private ReportReason reason = ReportReason.BAD_CONTENT;

    // 관리자 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReportProcessingStatus status = ReportProcessingStatus.PENDING;
}
