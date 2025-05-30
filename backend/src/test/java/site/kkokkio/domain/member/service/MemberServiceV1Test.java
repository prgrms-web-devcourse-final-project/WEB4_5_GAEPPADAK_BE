package site.kkokkio.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.controller.dto.MemberUpdateRequest;
import site.kkokkio.domain.member.controller.dto.PasswordResetRequest;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.repository.MemberRepository;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.util.JwtUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceV1Test {

	@InjectMocks
	private MemberService memberService;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtils jwtUtils;

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	@DisplayName("회원가입 성공")
	void createMember_success() {
		// given
		String email = "user@example.com";
		String nickname = "tester";
		LocalDate birthDate = LocalDate.of(1990, 1, 1);
		String rawPassword = "pw";
		String encPassword = "hashed-pw";

		MemberSignUpRequest request = new MemberSignUpRequest(email, rawPassword, nickname, birthDate);

		given(memberRepository.existsByEmail(email)).willReturn(false);
		given(memberRepository.existsByNickname(nickname)).willReturn(false);
		given(passwordEncoder.encode(rawPassword)).willReturn(encPassword);
		given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

		// when
		MemberResponse response = memberService.createMember(request);

		// then
		assertThat(response.getEmail()).isEqualTo(email);
		assertThat(response.getNickname()).isEqualTo(nickname);
	}

	@Test
	@DisplayName("이메일 중복 시 회원가입 실패")
	void createMember_fail_duplicateEmail() {
		given(memberRepository.existsByEmail("dup@example.com")).willReturn(true);

		assertThatThrownBy(() -> memberService.createMember(
			new MemberSignUpRequest("dup@example.com", "pw", "nick", LocalDate.now())))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("이미 사용중인 이메일");
	}

	@Test
	@DisplayName("닉네임 중복 시 회원가입 실패")
	void createMember_fail_duplicateNickname() {
		given(memberRepository.existsByEmail("email@example.com")).willReturn(false);
		given(memberRepository.existsByNickname("dupNick")).willReturn(true);

		assertThatThrownBy(() -> memberService.createMember(
			new MemberSignUpRequest("email@example.com", "pw", "dupNick", LocalDate.now())))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("이미 사용중인 닉네임");
	}

	@Test
	@DisplayName("이메일로 회원 조회 - 성공")
	void findByEmail_success() {
		Member member = Member.builder()
			.email("find@test.com")
			.nickname("nick")
			.build();

		given(memberRepository.findByEmail("find@test.com")).willReturn(Optional.of(member));

		Member found = memberService.findByEmail("find@test.com");

		assertThat(found.getEmail()).isEqualTo("find@test.com");
	}

	@Test
	@DisplayName("이메일로 회원 조회 - 실패")
	void findByEmail_fail() {
		given(memberRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

		assertThatThrownBy(() -> memberService.findByEmail("notfound@test.com"))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("존재하지 않는 이메일");
	}

	@Test
	@DisplayName("회원정보 조회 - 유효한 토큰")
	void getMemberInfo_success() {
		// given
		String email = "user@example.com";

		Member member = Member.builder()
			.email(email)
			.nickname("tester")
			.birthDate(LocalDate.of(1990, 1, 1))
			.role(MemberRole.USER)
			.build();

		given(memberRepository.findByEmail(email))
			.willReturn(Optional.of(member));

		// when
		MemberResponse resp = memberService.getMemberInfo(email);

		// then
		assertThat(resp).isNotNull();
		assertThat(resp.getEmail()).isEqualTo(email);
		assertThat(resp.getNickname()).isEqualTo("tester");
		assertThat(resp.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
		assertThat(resp.getRole()).isEqualTo(MemberRole.USER);
	}

	@Test
	@DisplayName("회원정보 조회 - 토큰 누락")
	void getMemberInfo_noToken_throws() {
		String email = "nonexistent@example.com";

		given(memberRepository.findByEmail(email))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> memberService.getMemberInfo(email))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("존재하지 않는 이메일");
	}

	@Test
	@DisplayName("회원정보 수정 성공")
	void modifyMemberInfo_success() {
		Member member = Member.builder()
			.id(UUID.randomUUID())
			.email("user@example.com")
			.nickname("tester")
			.passwordHash("encodedPassword")
			.birthDate(LocalDate.of(1990, 1, 1))
			.role(MemberRole.USER)
			.emailVerified(true)
			.build();

		CustomUserDetails userDetails = new CustomUserDetails(
			member.getEmail(), member.getRole().toString(), member.isEmailVerified());
		when(memberRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));

		MemberUpdateRequest request = new MemberUpdateRequest("password0000!", "change");

		MemberResponse response = memberService.modifyMemberInfo(userDetails, request);

		assertThat(response.getEmail()).isEqualTo("user@example.com");
		assertThat(response.getNickname()).isEqualTo("change");
	}

	@Test
	@DisplayName("회원정보 수정 실패 - 토큰 누락")
	void modifyMemberInfo_fail_tokenExpired() {
		// given
		MemberUpdateRequest request = new MemberUpdateRequest("change", "newPassword");

		// when & then
		assertThatThrownBy(() -> memberService.modifyMemberInfo(null, request))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("userDetails");

	}

	@Test
	@DisplayName("회원정보 수정 실패 - 닉네임 중복")
	void modifyMemberInfo_fail_nicknameDuplicate() {
		// given
		Member member = Member.builder()
			.id(UUID.randomUUID())
			.email("user@example.com")
			.nickname("originalNickname")
			.passwordHash("encodedPassword")
			.build();

		CustomUserDetails userDetails = new CustomUserDetails(
			member.getEmail(), member.getRole().toString(), member.isEmailVerified());
		when(memberRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
		// 새로운 닉네임이 이미 존재한다고 Mock
		when(memberRepository.existsByNickname("existingNickname")).thenReturn(true);

		MemberUpdateRequest request = new MemberUpdateRequest(null, "existingNickname"); // 닉네임만 변경 요청

		// when & then
		assertThatThrownBy(() -> memberService.modifyMemberInfo(userDetails, request))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("이미 사용중인 닉네임입니다.")
			.extracting("code").isEqualTo("409");

		// verify that save was not called
		verify(memberRepository, never()).save(any(Member.class));
		verify(memberRepository, times(1)).existsByNickname("existingNickname");
	}

	@Test
	@DisplayName("회원정보 수정 실패 - 현재 닉네임과 동일")
	void modifyMemberInfo_fail_nicknameSameAsCurrent() {
		// given
		Member member = Member.builder()
			.id(UUID.randomUUID())
			.email("user@example.com")
			.nickname("tester")
			.passwordHash("encodedPassword")
			.build();

		CustomUserDetails userDetails = new CustomUserDetails(
			member.getEmail(), member.getRole().toString(), member.isEmailVerified());
		when(memberRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));

		MemberUpdateRequest request = new MemberUpdateRequest(null, "tester"); // 현재 닉네임과 동일한 닉네임으로 변경 요청

		// when & then
		assertThatThrownBy(() -> memberService.modifyMemberInfo(userDetails, request))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("현재 닉네임과 동일한 닉네임입니다.")
			.extracting("code").isEqualTo("400");

		// verify
		verify(memberRepository, never()).existsByNickname(anyString());
		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("회원정보 수정 실패 - 현재 비밀번호와 동일")
	void modifyMemberInfo_fail_passwordSameAsCurrent() {
		// given
		Member member = Member.builder()
			.id(UUID.randomUUID())
			.email("user@example.com")
			.nickname("tester")
			.passwordHash("encodedPassword") // Mocking에서 사용할 해시된 비밀번호
			.build();

		CustomUserDetails userDetails = new CustomUserDetails(
			member.getEmail(), member.getRole().toString(), member.isEmailVerified());
		when(memberRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
		// passwordEncoder.matches가 현재 비밀번호와 동일하다고 Mock
		when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);

		MemberUpdateRequest request = new MemberUpdateRequest("currentPassword", null); // 현재 비밀번호와 동일한 비밀번호로 변경 요청

		// when & then
		assertThatThrownBy(() -> memberService.modifyMemberInfo(userDetails, request))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("현재 비밀번호와 동일한 비밀번호입니다.")
			.extracting("code").isEqualTo("400");

		// verify that save was not called
		verify(memberRepository, never()).save(any(Member.class));
		verify(passwordEncoder, times(1)).matches("currentPassword", "encodedPassword");
		verify(passwordEncoder, never()).encode(anyString());
	}

	@Test
	@DisplayName("회원 탈퇴 성공")
	void deleteMember_success() {
		// given
		Member member = Mockito.spy(Member.builder()
			.id(UUID.randomUUID())
			.email("test@example.com")
			.nickname("사용자")
			.build());
		UserDetails userDetails = new CustomUserDetails(member.getEmail(), "USER", true);
		when(memberRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));

		// when
		memberService.deleteMember(userDetails);

		// then
		verify(member).maskPersonalInfo();
		verify(member).softDelete();
		verify(memberRepository).save(member);
	}

	@Test
	@DisplayName("비밀번호 초기화 - 성공")
	void resetPassword_success() {
		// given
		String email = "user@example.com";
		String key = "EMAIL_VERIFIED:" + email;
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(key)).willReturn("true"); // 인증 상태

		String newPassword = "newPass123!";
		String encryptedPassword = "encrypted-hash";
		PasswordResetRequest req = new PasswordResetRequest(email, newPassword);

		given(memberRepository.findByEmail(email)).willReturn(Optional.of(
			Member.builder().email(email).nickname("nick").build()
		));
		given(passwordEncoder.encode(newPassword)).willReturn(encryptedPassword);

		// when
		memberService.resetPassword(req);

		// then : 비밀번호 암호화되어 저장, Redis 키 삭제 여부 확인
		assertThat(memberRepository.findByEmail(email).get().getPasswordHash()).isEqualTo(encryptedPassword);
		verify(memberRepository).save(any(Member.class));
		verify(redisTemplate).delete(key);
	}

	@Test
	@DisplayName("비밀번호 초기화 - 인증 미완료")
	void resetPassword_notVerified_fail() {
		// given
		String email = "user@example.com";
		String key = "EMAIL_VERIFIED:" + email;
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(key)).willReturn(null); // 미인증 상태

		// when
		PasswordResetRequest req = new PasswordResetRequest(email, "password1!");

		// then : 인증 미완료 예외 발생 여부 확인
		assertThatThrownBy(() -> memberService.resetPassword(req))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("인증코드가 유효하지 않습니다.");
	}

	@Test
	@DisplayName("비밀번호 초기화 - 이메일 없음")
	void resetPassword_emailNotFound_fail() {
		// given
		String email = "unknown@example.com";
		String key = "EMAIL_VERIFIED:" + email;
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(key)).willReturn("true");

		// when
		PasswordResetRequest req = new PasswordResetRequest(email, "newPass1");

		given(memberRepository.findByEmail(email)).willReturn(Optional.empty()); // 존재하지 않는 이메일 입력

		// then
		assertThatThrownBy(() -> memberService.resetPassword(req))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("존재하지 않는 이메일입니다.");
	}

}
