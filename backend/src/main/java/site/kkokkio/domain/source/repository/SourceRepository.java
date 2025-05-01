package site.kkokkio.domain.source.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.kkokkio.domain.source.entity.Source;

@Repository
public interface SourceRepository extends JpaRepository<Source, String> {
}
