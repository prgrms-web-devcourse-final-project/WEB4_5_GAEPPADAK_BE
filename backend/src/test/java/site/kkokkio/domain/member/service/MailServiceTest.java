package site.kkokkio.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import site.kkokkio.domain.member.controller.dto.EmailVerificationRequest;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.repository.MemberRepository;
import site.kkokkio.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
public class MailServiceTest {

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private MimeMessage mimeMessage;

	@InjectMocks
	private MailService mailService;

	private String testEmail;
	private String testAuthCode;
	private Member testMember;

	@BeforeEach
	void setUp() {
		testEmail = "test@example.com";
		testAuthCode = "ABC123";

		testMember = Member.builder()
			.id(UUID.randomUUID())
			.email(testEmail)
			.passwordHash("encryptedPassword")
			.nickname("testuser")
			.build();

		// MailService에 senderEmail 필드 주입
		ReflectionTestUtils.setField(mailService, "senderEmail", "sender@kkokkio.site");

		// StringRedisTemplate의 opsForValue()가 valueOperations를 반환하도록 설정
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	@Test
	@DisplayName("인증 코드 생성 테스트")
	void createCodeTest() {
		// when
		String code = mailService.createCode();

		// then
		assertThat(code).isNotNull();
		assertThat(code).hasSize(6);
		assertThat(code).matches("[A-Z0-9]{6}");
	}

	@Test
	@DisplayName("인증 메일 생성 테스트")
	void createAuthCodeMessageTest() throws MessagingException {
		// given
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

		// when
		MimeMessage result = mailService.createAuthCodeMessage(testEmail, testAuthCode);

		// then
		assertThat(result).isEqualTo(mimeMessage);
		verify(mailSender, times(1)).createMimeMessage();
	}

	@Test
	@DisplayName("메일 발송 성공 테스트")
	void sendSimpleMessageSuccessTest() throws MessagingException {
		// given
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

		// when
		String result = mailService.sendSimpleMessage(testEmail);

		// then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(6);
		verify(mailSender, times(1)).createMimeMessage();
		verify(mailSender, times(1)).send(any(MimeMessage.class));
	}

	@Test
	@DisplayName("메일 발송 실패 테스트")
	void sendSimpleMessageFailTest() throws MessagingException {
		// given
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
		doThrow(new MailException("Mail sending failed") {
		}).when(mailSender).send(any(MimeMessage.class));

		// when
		String result = mailService.sendSimpleMessage(testEmail);

		// then
		assertThat(result).isNull();
		verify(mailSender, times(1)).createMimeMessage();
		verify(mailSender, times(1)).send(any(MimeMessage.class));
	}

	@Test
	@DisplayName("인증 코드 발송 성공 테스트")
	void sendAuthCodeSuccessTest() throws MessagingException {
		// given
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

		// when
		boolean result = mailService.sendAuthCode(testEmail);

		// then
		assertThat(result).isTrue();
		verify(mailSender, times(1)).createMimeMessage();
		verify(mailSender, times(1)).send(any(MimeMessage.class));
		verify(valueOperations, times(1)).set(
			eq("EMAIL_AUTH:" + testEmail),
			anyString(),
			eq(Duration.ofSeconds(300))
		);
	}

	@Test
	@DisplayName("인증 코드 발송 실패 테스트")
	void sendAuthCodeFailTest() throws MessagingException {
		// given
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
		doThrow(new MailException("Mail sending failed") {
		}).when(mailSender).send(any(MimeMessage.class));

		// when
		boolean result = mailService.sendAuthCode(testEmail);

		// then
		assertThat(result).isFalse();
		verify(mailSender, times(1)).createMimeMessage();
		verify(mailSender, times(1)).send(any(MimeMessage.class));
		verify(valueOperations, times(0)).set(anyString(), anyString(), any(Duration.class));
	}

	@Test
	@DisplayName("인증 코드 검증 성공 테스트")
	void validationAuthCodeSuccessTest() {
		// given
		EmailVerificationRequest request = new EmailVerificationRequest(testEmail, testAuthCode);
		when(valueOperations.get("EMAIL_AUTH:" + testEmail)).thenReturn(testAuthCode);
		when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(testMember));

		// when & then
		assertDoesNotThrow(() -> mailService.validationAuthCode(request));
		verify(valueOperations, times(1)).get("EMAIL_AUTH:" + testEmail);
		verify(memberRepository, times(1)).findByEmail(testEmail);
		verify(memberRepository, times(1)).save(testMember);
		verify(redisTemplate, times(1)).delete("EMAIL_AUTH:" + testEmail);
		assertThat(testMember.isEmailVerified()).isTrue();
	}

	@Test
	@DisplayName("인증 코드 검증 실패 - 인증 코드 불일치")
	void validationAuthCodeFailByMismatchTest() {
		// given
		EmailVerificationRequest request = new EmailVerificationRequest(testEmail, testAuthCode);
		when(valueOperations.get("EMAIL_AUTH:" + testEmail)).thenReturn("WRONG123");

		// when & then
		assertThatThrownBy(() -> mailService.validationAuthCode(request))
			.isInstanceOf(ServiceException.class)
			.hasMessage("인증 코드가 유효하지 않습니다.");
		verify(valueOperations, times(1)).get("EMAIL_AUTH:" + testEmail);
		verify(memberRepository, times(0)).findByEmail(anyString());
		verify(memberRepository, times(0)).save(any(Member.class));
		verify(redisTemplate, times(0)).delete(anyString());
	}

	@Test
	@DisplayName("인증 코드 검증 실패 - 저장된 인증 코드 없음")
	void validationAuthCodeFailByNoStoredCodeTest() {
		// given
		EmailVerificationRequest request = new EmailVerificationRequest(testEmail, testAuthCode);
		when(valueOperations.get("EMAIL_AUTH:" + testEmail)).thenReturn(null);

		// when & then
		assertThatThrownBy(() -> mailService.validationAuthCode(request))
			.isInstanceOf(ServiceException.class)
			.hasMessage("인증 코드가 유효하지 않습니다.");
		verify(valueOperations, times(1)).get("EMAIL_AUTH:" + testEmail);
		verify(memberRepository, times(0)).findByEmail(anyString());
		verify(memberRepository, times(0)).save(any(Member.class));
		verify(redisTemplate, times(0)).delete(anyString());
	}
}