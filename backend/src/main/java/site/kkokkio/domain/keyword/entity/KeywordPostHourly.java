package site.kkokkio.domain.keyword.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.global.util.BaseTimeEntity;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "keyword_post_hourly")
public class KeywordPostHourly extends BaseTimeEntity {

    @EmbeddedId
    private KeywordPostHourlyId id;

    @MapsId("keywordMetricHourlyId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // ON DELETE CASCADE
    @JoinColumns({
        @JoinColumn(name = "bucket_at", referencedColumnName = "bucket_at"),
        @JoinColumn(name = "platform", referencedColumnName = "platform"),
        @JoinColumn(name = "keyword_id", referencedColumnName = "keyword_id")
    })
    private KeywordMetricHourly keywordMetricHourly;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // ON DELETE CASCADE
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}
