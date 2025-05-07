package site.kkokkio.domain.source.repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.source.entity.KeywordSource;

@Repository
@RequiredArgsConstructor
public class KeywordSourceRepositoryCustomImpl implements KeywordSourceRepositoryCustom {

    private final EntityManager em;

    @Transactional
    @Override
    public void insertIgnoreAll(List<KeywordSource> mappings) {
        if (mappings.isEmpty()) return;

        StringBuilder sql = new StringBuilder("""
            INSERT IGNORE INTO keyword_source
            (keyword_id, fingerprint, created_at, updated_at)
            VALUES
        """);

        for (int i = 0; i < mappings.size(); i++) {
            sql.append("(?, ?, NOW(), NOW())");
            if (i < mappings.size() - 1) sql.append(", ");
        }

        Query query = em.createNativeQuery(sql.toString());

        int index = 1;
        for (KeywordSource ks : mappings) {
            query.setParameter(index++, ks.getKeyword().getId());
            query.setParameter(index++, ks.getSource().getFingerprint());
        }

        query.executeUpdate();
    }
}
