package site.kkokkio.global.init;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.repository.MemberRepository;
import site.kkokkio.global.enums.MemberRole;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaseInitData implements CommandLineRunner {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) throws Exception {
		initMember();
	}

	// 회원 등록
	@Transactional
	public void initMember() {
		if (memberRepository.count() > 0) {
			log.info("이미 회원 데이터가 존재합니다.");
			return;
		}

		// 회원1 추가
		Member member1 = Member.builder()
			.email("user1@example.com")
			.passwordHash(passwordEncoder.encode("password123!"))
			.nickname("user1")
			.birthDate(LocalDate.of(1990, 1, 1))
			.role(MemberRole.USER)
			.deletedAt(null)
			.emailVerified(true)
			.build();
		memberRepository.save(member1);

		// 회원2 추가
		Member member2 = Member.builder()
			.email("user2@example.com")
			.passwordHash(passwordEncoder.encode("password123!"))
			.nickname("user2")
			.birthDate(LocalDate.of(1990, 1, 1))
			.role(MemberRole.USER)
			.deletedAt(null)
			.emailVerified(true)
			.build();
		memberRepository.save(member2);

		// 회원3 추가
		Member member3 = Member.builder()
			.email("test3@example.com")
			.passwordHash(passwordEncoder.encode("password123!"))
			.nickname("test3")
			.birthDate(LocalDate.of(2000, 4, 10))
			.role(MemberRole.USER)
			.deletedAt(null)
			.emailVerified(true)
			.build();
		memberRepository.save(member3);

		// 관리자 추가
		Member admin1 = Member.builder()
			.email("admin1@example.com")
			.passwordHash(passwordEncoder.encode("password123!"))
			.nickname("admin1")
			.birthDate(LocalDate.of(2025, 5, 1))
			.role(MemberRole.ADMIN)
			.deletedAt(null)
			.emailVerified(true)
			.build();
		memberRepository.save(admin1);
		log.info("회원 데이터가 등록되었습니다.");
	}
}
