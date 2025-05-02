package site.kkokkio.domain.source.repository;

import java.util.List;

import site.kkokkio.domain.source.entity.KeywordSource;

public interface KeywordSourceRepositoryCustom {
    void insertIgnoreAll(List<KeywordSource> mappings);
}
