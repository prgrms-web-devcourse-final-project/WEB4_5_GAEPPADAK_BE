package site.kkokkio.domain.post.repository;

import java.util.List;

import site.kkokkio.domain.post.entity.PostKeyword;

public interface PostKeywordRepositoryCustom {
    void insertIgnoreAll(List<PostKeyword> mappings);
}
