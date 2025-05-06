package site.kkokkio.domain.source.entity;

import jakarta.persistence.*;
import lombok.*;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.util.BaseTimeEntity;
import site.kkokkio.global.util.HashUtils;

import java.time.LocalDateTime;

/**
 * INDEX (platform, fetched_at)
 */
@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(name = "source")
public class Source extends BaseTimeEntity {

    @EqualsAndHashCode.Include
    @Id
    @Column(length = 64)
    private String fingerprint;

    @Column(name = "normalized_url", length = 500, nullable = false)
    private String normalizedUrl;

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Setter
	@Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "video_id")
    private String videoId;

    /**
     * persist 직전에 URL을 해싱해 fingerprint를 자동 할당
     */
    @PrePersist
    public void ensureFingerprint() {
        if (this.fingerprint == null && this.normalizedUrl != null) {
            this.fingerprint = HashUtils.sha256Hex(this.normalizedUrl);
        }
    }
}