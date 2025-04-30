package site.kkokkio.domain.source.entity;

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
 * UNIQUE KEY (keyword_id, fingerprint)
 */
@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "keyword_source")
public class KeywordSource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_source_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // ON DELETE CASCADE
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // ON DELETE CASCADE
    @JoinColumn(name = "fingerprint", nullable = false)
    private Source source;
}