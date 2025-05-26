package site.kkokkio.domain.post.repository;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.dto.ReportedPostSummary;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostReport;
import site.kkokkio.global.enums.ReportProcessingStatus;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

	// 특정 사용자가 특정 포스트를 이미 신고했는지 확인하는 메소드
	boolean existsByPostAndReporter(Post post, Member reporter);

	// 관리자용 신고된 포스트 목록 집계 조회 메서드
	@Query(value = """
		SELECT
				p.post_id AS postId,
				p.title AS title,
				p.summary AS summary,
				pk.keyword_id AS keywordId,
				k.text AS keyword,
				GROUP_CONCAT(pr.reason SEPARATOR ',') AS reportReasons,
				MAX(pr.created_at) AS latestReportedAt,
				COUNT(pr.post_report_id) AS reportCount,
				pr.status AS status
		FROM post_report pr
		JOIN post p ON pr.post_id = p.post_id
		LEFT JOIN post_keyword pk ON pk.post_id = pr.post_id
		LEFT JOIN keyword k ON k.keyword_id = pk.keyword_id
		WHERE (:searchTitle IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTitle, '%')))
			AND (:searchSummary IS NULL OR LOWER(p.summary) LIKE LOWER(CONCAT('%', :searchSummary, '%')))
			AND (:searchKeyword IS NULL OR LOWER(k.text) LIKE LOWER(CONCAT('%', :searchKeyword, '%')))
			AND (p.deleted_at IS NULL)
			AND (:searchReportReason IS NULL OR p.post_id IN (
					SELECT sub_pr.post_id
					FROM post_report sub_pr
					WHERE sub_pr.post_id = p.post_id AND LOWER(sub_pr.reason)
					LIKE LOWER(CONCAT('%', :searchReportReason, '%'))
				))
		GROUP BY
				p.post_id,
				p.title,
				p.summary,
				pk.keyword_id,
				k.text,
				pr.status
		""",
		countQuery = """
			SELECT COUNT(DISTINCT pr.post_id)
			FROM post_report pr
			JOIN post p ON pr.post_id = p.post_id
			LEFT JOIN post_keyword pk ON pk.post_id = pr.post_id
			LEFT JOIN keyword k ON k.keyword_id = pk.keyword_id
			WHERE (:searchTitle IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTitle, '%')))
			AND (:searchSummary IS NULL OR LOWER(p.summary) LIKE LOWER(CONCAT('%', :searchSummary, '%')))
			AND (:searchKeyword IS NULL OR LOWER(k.text) LIKE LOWER(CONCAT('%', :searchKeyword, '%')))
			AND (p.deleted_at IS NULL)
			AND (:searchReportReason IS NULL OR p.post_id IN (
					SELECT sub_pr.post_id
					FROM post_report sub_pr
					WHERE sub_pr.post_id = p.post_id AND LOWER(sub_pr.reason)
					LIKE LOWER(CONCAT('%', :searchReportReason, '%'))
				))
			""",
		nativeQuery = true
	)
	Page<ReportedPostSummary> findReportedPostSummary(
		@Param("searchTitle") String searchTitle,
		@Param("searchSummary") String searchSummary,
		@Param("searchKeyword") String searchKeyword,
		@Param("searchReportReason") String searchReportReason,
		Pageable pageable
	);

	// 주어진 포스트 ID 목록에 해당하는 모든 신고 엔티티의 상태를 일괄 업데이트하는 메서드
	@Modifying
	@Query("UPDATE PostReport pr SET pr.status = :status WHERE pr.post.id IN :postIds")
	void updateStatusByPostIdIn(
		@Param("postIds") Collection<Long> postIds,
		@Param("status") ReportProcessingStatus status);

	// 주어진 포스트 ID 목록 중 신고된 포스트의 개수를 세는 메서드
	@Query("SELECT COUNT(DISTINCT pr.post.id) FROM PostReport pr WHERE pr.post.id IN :postIds")
	long countByPostIdIn(@Param("postIds") Collection<Long> postIds);
}
