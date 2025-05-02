package site.kkokkio.domain.source.entity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(name = "keyword_source")
public class KeywordSource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_source_id")
    private Long id;

    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // ON DELETE CASCADE
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // ON DELETE CASCADE
    @JoinColumn(name = "fingerprint", nullable = false)
    private Source source;

    public static Map<Long, List<Source>> groupByKeywordId(List<KeywordSource> list) {
        return list.stream()
            .collect(Collectors.groupingBy(
                ks -> ks.getKeyword().getId(),
                Collectors.mapping(KeywordSource::getSource, Collectors.toList())
            ));
    }
}