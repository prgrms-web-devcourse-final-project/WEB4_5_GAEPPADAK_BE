package site.kkokkio.domain.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.global.util.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "comment")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // ON DELETE CASCADE
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

	@Builder.Default
    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

	@Builder.Default
    @Column(name = "is_hidden", nullable = false)
    private boolean hidden = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // report_count 추가
    @Builder.Default
    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	public void updateBody(String body) {
		this.body = body;
	}

	public void increaseLikeCount() {
		this.likeCount++;
	}

	public void decreaseLikeCount() {
		this.likeCount--;
	}

    // 신고 카운트를 증가시키는 메서드
    public void increaseReportCount() { this.reportCount++; }
}
