package site.kkokkio.domain.member.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.repository.MemberRepository;
import site.kkokkio.global.exception.ServiceException;

@Service
@RequiredArgsConstructor
public class MailService {

	private final JavaMailSender mailSender;
	private final StringRedisTemplate redisTemplate;
	private final MemberRepository memberRepository;

	@Value("${spring.mail.username}")
	String senderEmail;

	// Redis에 키 저장 시 접두어
	private static final String EMAIL_AUTH_PREFIX = "EMAIL_AUTH:";

	// 인증 코드 생성
	public String createCode() {
		Random random = new Random();
		StringBuilder key = new StringBuilder();

		for (int i = 0; i < 6; i++) { // 인증 코드 6자리
			int index = random.nextInt(2); // 0~1까지 랜덤, 랜덤값으로 switch문 실행

			switch (index) {
				case 0 -> key.append((char)(random.nextInt(26) + 65)); // 대문자
				case 1 -> key.append(random.nextInt(10)); // 숫자
			}
		}
		return key.toString();
	}

	// 메일 생성
	public MimeMessage createAuthCodeMessage(String recipientEmail, String authCode)
		throws MessagingException {

		MimeMessage message = mailSender.createMimeMessage();

		// 두 번째 인자로 true를 주면 multipart 메시지(Html + inline/attachment) 지원
		MimeMessageHelper helper = new MimeMessageHelper(
			message,
			true,
			StandardCharsets.UTF_8.name() // 한글 깨짐 방지를 위한 인코딩
		);

		helper.setFrom(senderEmail); // 보내는 주소
		helper.setTo(recipientEmail);// 받는 주소
		helper.setSubject("이메일 인증 코드 안내");

		// 메일 본문
		String htmlBody = """
			<div style="font-family:Arial,sans-serif; line-height:1.6;">
			  <h3>안녕하세요! 꼬끼오입니다.</h3>
			  <p>요청하신 인증 번호입니다:</p>
			  <h1 style="color:#2a9d8f;">%s</h1>
			  <p>위 번호를 화면에 입력해 주세요.</p>
			  <hr>
			  <small>감사합니다.</small>
			</div>
			""".formatted(authCode);

		helper.setText(htmlBody, /* isHtml */ true);
		return message;
	}

	// 메일 발송
	public String sendSimpleMessage(String sendEmail) throws MessagingException {
		String authCode = createCode(); // 랜덤 인증번호 생성

		MimeMessage message = createAuthCodeMessage(sendEmail, authCode); // 메일 생성
		try {
			mailSender.send(message); // 메일 발송
			return authCode;
		} catch (MailException e) {
			return null;
		}
	}

	// 인증 코드 발송
	@Transactional
	public boolean sendAuthCode(String email) throws MessagingException {

		// DB에서 회원 이메일이 존재하는지 확인
		if (!memberRepository.existsByEmail(email)) {
			throw new ServiceException("404", "존재하지 않는 회원입니다.");
		}

		String authCode = sendSimpleMessage(email); // 이메일 인증 코드 발송

		if (authCode != null) {
			storeAuthCodeInRedis(email, authCode);
			return true;
		}
		return false;
	}

	// 인증 코드 Redis 저장
	@Transactional
	public void storeAuthCodeInRedis(String email, String authCode) {
		// Redis에 인증 코드 저장 (만료 시간 설정)
		ValueOperations<String, String> values = redisTemplate.opsForValue();
		// 키 이름에 접두어 추가해서 저장
		String key = EMAIL_AUTH_PREFIX + email;

		// Redis에 저장하고 만료 시간 설정 (밀리초를 초 단위로 변환)
		values.set(key, authCode, Duration.ofSeconds(300)); // 5분
	}

	// 인증 코드 검증
	@Transactional
	public boolean verifyAuthCode(String email, String authCode) {
		String key = EMAIL_AUTH_PREFIX + email;
		ValueOperations<String, String> values = redisTemplate.opsForValue();
		String storedCode = values.get(key);

		// 코드 검증
		if (storedCode == null || !storedCode.equals(authCode)) {
			return false;
		}

		// 인증 상태를 Redis에 저장
		redisTemplate.opsForValue().set("EMAIL_VERIFIED:" + email, "true", Duration.ofMinutes(5));// 5분간 저장
		redisTemplate.delete(key);
		return true;
	}

	// 회원 가입 이메일 인증
	@Transactional
	public void confirmSignup(String email, String authCode) {
		if (!verifyAuthCode(email, authCode)) {
			throw new ServiceException("401", "인증 코드가 유효하지 않습니다.");
		}
		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 이메일입니다."));
		member.setEmailVerified(true);
		memberRepository.save(member);
		// 이메일 인증 플래그 삭제
		redisTemplate.delete("EMAIL_VERIFIED:" + email);
	}

}
