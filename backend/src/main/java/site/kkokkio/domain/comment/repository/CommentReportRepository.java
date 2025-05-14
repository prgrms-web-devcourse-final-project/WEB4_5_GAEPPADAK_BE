package site.kkokkio.domain.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.comment.entity.CommentReport;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    // 특정 사용자가 특정 댓글을 이미 신고했는지 확인하는 메서드
    boolean existsByCommentAndReporter(Comment comment, Member reporter);
}
