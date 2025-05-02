package site.kkokkio.domain.source.repository;

import java.util.List;

import site.kkokkio.domain.source.entity.PostSource;

public interface PostSourceRepositoryCustom {
    void insertIgnoreAll(List<PostSource> mappings);
}
