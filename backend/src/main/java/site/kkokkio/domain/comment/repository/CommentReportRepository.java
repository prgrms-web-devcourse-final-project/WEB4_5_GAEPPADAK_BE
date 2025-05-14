package site.kkokkio.domain.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import site.kkokkio.domain.comment.dto.ReportedCommentSummary;
import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.comment.entity.CommentReport;
import site.kkokkio.domain.member.entity.Member;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

	// 특정 사용자가 특정 댓글을 이미 신고했는지 확인하는 메서드
	boolean existsByCommentAndReporter(Comment comment, Member reporter);

	// 관리자용 신고된 댓글 목록 집계 조회 메서드
	@Query(value = """
		SELECT
				c.comment_id AS commentId,
				m.member_id AS memberId,
				m.nickname AS nickname,
				(m.deleted_at IS NOT NULL) AS isDeletedMember,
				p.post_id AS postId,
				p.title AS postTitle,
				c.body AS commentBody,
				GROUP_CONCAT(cr.reason SEPARATOR ',') AS reportReasons,
				MAX(cr.created_at) AS latestReportedAt,
				COUNT(cr.comment_report_id) AS reportCount
			FROM comment_report cr
			JOIN comment c ON cr.comment_id = c.comment_id
			JOIN post p ON c.post_id = p.post_id
			JOIN member m ON c.member_id = m.member_id
			WHERE (:searchNickname IS NULL OR LOWER(m.nickname) LIKE LOWER(CONCAT('%', :searchNickname, '%')))
				AND (:searchPostTitle IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchPostTitle, '%')))
				AND (:searchCommentBody IS NULL OR LOWER(c.body) LIKE LOWER(CONCAT('%', :searchCommentBody, '%')))
				AND (:searchReportReason IS NULL OR LOWER(cr.reason) LIKE LOWER(CONCAT('%', :searchReportReason, '%')))
				AND (c.deleted_at IS NULL)
				AND (c.is_hidden = FALSE)
			GROUP BY
					c.comment_id, m.member_id, m.nickname, m.deleted_at, p.post_id, p.title, c.body
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
				AND (:searchReportReason IS NULL OR LOWER(cr.reason) LIKE LOWER(CONCAT('%', :searchReportReason, '%')))
				AND (c.deleted_at IS NULL)
				AND (c.is_hidden = FALSE)
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
}
