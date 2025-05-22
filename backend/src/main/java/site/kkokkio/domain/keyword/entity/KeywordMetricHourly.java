package site.kkokkio.domain.keyword.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.global.util.BaseTimeEntity;

/**
 * INDEX (bucket_at, score)
 */
@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "keyword_metric_hourly")
public class KeywordMetricHourly extends BaseTimeEntity {

    @EmbeddedId
    private KeywordMetricHourlyId id;

    @MapsId("keywordId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // ON DELETE CASCADE
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Setter
	@ManyToOne(fetch = FetchType.LAZY) // ON DELETE SET NULL
    @JoinColumn(name = "post_id")
    private Post post;

    @Builder.Default
    @Column(nullable = false)
    private int volume = 0;

    @Builder.Default
    @Column(nullable = false)
    private int score = 0;

    @Builder.Default
    @Column(name = "rank_delta", nullable = false)
    private double rankDelta = 0.0;

    @Builder.Default
    @Column(name = "novelty_ratio", nullable = false)
    private double noveltyRatio = 1.0;

    @Builder.Default
    @Column(name = "weighted_novelty", nullable = false)
    private double weightedNovelty = 10;

    @Builder.Default
    @Column(name = "no_post_streak", nullable = false)
    private int noPostStreak = 0;

    @Builder.Default
    @Column(name = "low_variation", nullable = false)
    private boolean lowVariation = false;
}