package site.kkokkio.domain.post.entity;

import jakarta.persistence.*;
import lombok.*;
import site.kkokkio.global.util.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "post")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    public Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "bucket_at", nullable = false)
    private LocalDateTime bucketAt;

    @Builder.Default
    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    public void incrementReportCount() {
        this.reportCount++;
    }
}
