package site.kkokkio.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostReport;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    // 특정 사용자가 특정 포스트를 이미 신고했는지 확인하는 메소드
    boolean existsByPostAndReporter(Post post, Member reporter);
}
