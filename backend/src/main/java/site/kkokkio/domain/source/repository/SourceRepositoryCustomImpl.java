package site.kkokkio.domain.source.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import site.kkokkio.domain.source.entity.Source;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SourceRepositoryCustomImpl implements SourceRepositoryCustom {

    private final EntityManager em;

    @Transactional
    @Override
    public void insertIgnoreAll(List<Source> sources) {
        if (sources.isEmpty()) return;

        StringBuilder sql = new StringBuilder("""
            INSERT IGNORE INTO source
            (fingerprint, normalized_url, title, description, thumbnail_url, published_at, platform, video_id, created_at, updated_at)
            VALUES
        """);

        for (int i = 0; i < sources.size(); i++) {
            sql.append("(?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())");
            if (i < sources.size() - 1) sql.append(", ");
        }

        Query query = em.createNativeQuery(sql.toString());

        int index = 1;
        for (Source s : sources) {
            query.setParameter(index++, s.getFingerprint());
            query.setParameter(index++, s.getNormalizedUrl());
            query.setParameter(index++, s.getTitle());
            query.setParameter(index++, s.getDescription());
            query.setParameter(index++, s.getThumbnailUrl());
            query.setParameter(index++, s.getPublishedAt());
            query.setParameter(index++, s.getPlatform().name());
            query.setParameter(index++, s.getVideoId());
        }

        query.executeUpdate();
    }
}
