package site.kkokkio.global.init;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
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
@Profile({"local", "dev"})
public class BaseInitData implements CommandLineRunner {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	private final JobLauncher jobLauncher;
	private final Job trendingKeywordsJob;

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

	@EventListener(ApplicationReadyEvent.class)
	public void launchOnceOnStartup() throws
		JobExecutionAlreadyRunningException,
		JobRestartException,
		JobInstanceAlreadyCompleteException,
		JobParametersInvalidException {

		LocalDateTime bucketAt = LocalDateTime.now(ZoneId.of("UTC"))
			.withSecond(0).withNano(0);
		JobParameters params = new JobParametersBuilder()
			.addString("runTime", bucketAt.toString())
			.toJobParameters();

		log.info("Application ready — launching trendingKeywordsJob with runTime={}", bucketAt);
		jobLauncher.run(trendingKeywordsJob, params);
	}
}
