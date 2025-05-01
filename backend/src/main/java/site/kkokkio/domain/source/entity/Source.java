package site.kkokkio.domain.source.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.util.BaseTimeEntity;
import site.kkokkio.global.util.HashUtils;

/**
 * INDEX (platform, fetched_at)
 */
@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "source")
public class Source extends BaseTimeEntity {

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