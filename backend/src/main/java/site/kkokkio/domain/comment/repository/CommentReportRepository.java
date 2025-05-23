package site.kkokkio.domain.comment.repository;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import site.kkokkio.domain.comment.dto.ReportedCommentSummary;
import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.comment.entity.CommentReport;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.enums.ReportProcessingStatus;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

	// 특정 사용자가 특정 댓글을 이미 신고했는지 확인하는 메서드
	boolean existsByCommentAndReporter(Comment comment, Member reporter);

	// 관리자용 신고된 댓글 목록 집계 조회 메서드
	@Query(value = """
		SELECT
				c.comment_id AS commentId,
				BIN_TO_UUID(m.member_id) AS memberId,
				m.nickname AS nickname,
				(m.deleted_at IS NOT NULL) AS isDeletedMember,
				p.post_id AS postId,
				p.title AS postTitle,
				c.body AS commentBody,
				GROUP_CONCAT(cr.reason SEPARATOR ',') AS reportReasons,
				MAX(cr.created_at) AS latestReportedAt,
				COUNT(cr.comment_report_id) AS reportCount,
				cr.status AS status
			FROM comment_report cr
			JOIN comment c ON cr.comment_id = c.comment_id
			JOIN post p ON c.post_id = p.post_id
			JOIN member m ON c.member_id = m.member_id
			WHERE (:searchNickname IS NULL OR LOWER(m.nickname) LIKE LOWER(CONCAT('%', :searchNickname, '%')))
				AND (:searchPostTitle IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchPostTitle, '%')))
				AND (:searchCommentBody IS NULL OR LOWER(c.body) LIKE LOWER(CONCAT('%', :searchCommentBody, '%')))
				AND (c.deleted_at IS NULL)
				AND (c.is_hidden = FALSE)
				AND (:searchReportReason IS NULL OR c.comment_id IN (
					SELECT sub_cr.comment_id
					FROM comment_report sub_cr
					WHERE sub_cr.comment_id = c.comment_id AND LOWER(sub_cr.reason)
					LIKE LOWER(CONCAT('%', :searchReportReason, '%'))
				))
			GROUP BY
					c.comment_id, m.member_id, m.nickname, m.deleted_at, p.post_id, p.title, c.body, cr.status
		""",
		countQuery = """
				SELECT COUNT(DISTINCT cr.comment_id)
			FROM comment_report cr
			JOIN comment c ON cr.comment_id = c.comment_id
			JOIN post p ON c.post_id = p.post_id
			JOIN member m ON c.member_id = m.member_id
			WHERE (:searchNickname IS NULL OR LOWER(m.nickname) LIKE LOWER(CONCAT('%', :searchNickname, '%')))
				AND (:searchPostTitle IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchPostTitle, '%')))
				AND (:searchCommentBody IS NULL OR LOWER(c.body) LIKE LOWER(CONCAT('%', :searchCommentBody, '%')))
				AND (c.deleted_at IS NULL)
				AND (c.is_hidden = FALSE)
				AND (:searchReportReason IS NULL OR c.comment_id IN (
					SELECT sub_cr.comment_id
					FROM comment_report sub_cr
					WHERE sub_cr.comment_id = c.comment_id AND LOWER(sub_cr.reason)
					LIKE LOWER(CONCAT('%', :searchReportReason, '%'))
				))
			""",
		nativeQuery = true
	)
	Page<ReportedCommentSummary> findReportedCommentSummary(
		@Param("searchNickname") String searchNickname,
		@Param("searchPostTitle") String searchPostTitle,
		@Param("searchCommentBody") String searchCommentBody,
		@Param("searchReportReason") String searchReportReason,
		Pageable pageable
	);

	// 주어진 댓글 ID 목록에 해당하는 모든 신고 엔티티의 상태를 일괄 업데이트
	@Modifying
	@Query("UPDATE CommentReport cr SET cr.status = :status WHERE cr.comment.id IN :commentIds")
	void updateStatusByCommentIdIn(
		@Param("commentIds") Collection<Long> commentIds,
		@Param("status") ReportProcessingStatus status);
}
