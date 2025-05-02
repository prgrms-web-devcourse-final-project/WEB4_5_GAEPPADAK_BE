package site.kkokkio.domain.source.repository;

import java.util.List;

import site.kkokkio.domain.source.entity.Source;

public interface SourceRepositoryCustom {
    void insertIgnoreAll(List<Source> sources);
}
