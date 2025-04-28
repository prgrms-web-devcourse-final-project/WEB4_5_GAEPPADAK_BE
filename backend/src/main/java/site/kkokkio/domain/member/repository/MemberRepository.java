package site.kkokkio.domain.member.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import site.kkokkio.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, UUID> {
	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);
}
