package site.kkokkio.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.global.util.BaseTimeEntity;

/**
 * UNIQUE KEY (post_id, keyword_id)
 */
@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "post_keyword")
public class PostKeyword extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_keyword_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // ON DELETE CASCADE
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // ON DELETE CASCADE
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;
}
