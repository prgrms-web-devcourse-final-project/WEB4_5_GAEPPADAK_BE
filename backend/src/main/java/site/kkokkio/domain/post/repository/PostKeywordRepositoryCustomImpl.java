package site.kkokkio.domain.post.repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.post.entity.PostKeyword;

@Repository
@RequiredArgsConstructor
public class PostKeywordRepositoryCustomImpl implements PostKeywordRepositoryCustom {

    private final EntityManager em;

    @Transactional
    @Override
    public void insertIgnoreAll(List<PostKeyword> mappings) {
        if (mappings.isEmpty()) return;

        StringBuilder sql = new StringBuilder("""
            INSERT IGNORE INTO post_keyword
            (post_id, keyword_id, created_at, updated_at)
            VALUES
        """);

        for (int i = 0; i < mappings.size(); i++) {
            sql.append("(?, ?, NOW(), NOW())");
            if (i < mappings.size() - 1) sql.append(", ");
        }

        Query query = em.createNativeQuery(sql.toString());

        int index = 1;
        for (PostKeyword pk : mappings) {
            query.setParameter(index++, pk.getPost().getId());
            query.setParameter(index++, pk.getKeyword().getId());
        }

        query.executeUpdate();
    }
}
