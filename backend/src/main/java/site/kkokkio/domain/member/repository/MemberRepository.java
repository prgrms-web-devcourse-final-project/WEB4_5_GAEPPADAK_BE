package site.kkokkio.domain.member.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.enums.MemberRole;

public interface MemberRepository extends JpaRepository<Member, UUID> {
	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);

	Optional<Member> findByEmail(String email);

	// 닉네임으로 검색 (대소문자 구분 없이, 부분 일치)
	Page<Member> findByNicknameContainingIgnoreCase(String nickname, Pageable pageable);

	// 이메일로 검색 (대소문자 구분 없이, 부분 일치)
	Page<Member> findByEmailContainingIgnoreCase(String email, Pageable pageable);

	// 역할로 검색 (정확히 일치)
	Page<Member> findByRole(MemberRole role, Pageable pageable);

	// 닉네임 존재 여부 확인 (대소문자 구분 없이)
	boolean existsByNicknameIgnoreCase(String nickname);
}
