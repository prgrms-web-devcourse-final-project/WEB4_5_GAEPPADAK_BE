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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import site.kkokkio.domain.member.controller.dto.MemberLoginResponse;
import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.repository.MemberRepository;
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

	//--- 회원 가입 테스트 ----------------------------------

	@Test
	@DisplayName("회원 가입 성공")
	void createMember_success() {
		// given
		UUID id = UUID.randomUUID();
		String email = "user@example.com";
		String rawPassword = "plaintext";
		String nickname = "usernick";
		LocalDate birthDate = LocalDate.of(1990, 1, 1);

		MemberSignUpRequest request = new MemberSignUpRequest(email, rawPassword, nickname, birthDate);

		given(memberRepository.existsByEmail(email)).willReturn(false);
		given(memberRepository.existsByNickname(nickname)).willReturn(false);
		given(passwordEncoder.encode(rawPassword)).willReturn("encryptedPwd");

		Member saved = Member.builder()
			.id(id)
			.email(email)
			.passwordHash("encryptedPwd")
			.nickname(nickname)
			.birthDate(birthDate)
			.role(MemberRole.USER)
			.build();
		given(memberRepository.save(any(Member.class))).willReturn(saved);

		// when
		MemberResponse response = memberService.createMember(request);

		// then
		assertThat(response.getEmail()).isEqualTo(email);
		assertThat(response.getNickname()).isEqualTo(nickname);
		assertThat(response.getBirthDate()).isEqualTo(birthDate);
		assertThat(response.getRole()).isEqualTo(MemberRole.USER);
	}

	@Test
	@DisplayName("회원 가입 실패 - 이메일 중복")
	void createMember_fail_duplicateEmail() {
		// given
		given(memberRepository.existsByEmail("dup@example.com")).willReturn(true);

		// when & then
		ServiceException ex = catchThrowableOfType(
			() -> memberService.createMember(
				new MemberSignUpRequest("dup@example.com", "pw", "nick", LocalDate.now())),
			ServiceException.class);

		assertThat(ex.getCode()).isEqualTo("409-1");
		assertThat(ex.getMessage()).contains("이미 사용중인 이메일입니다.");
	}

	@Test
	@DisplayName("회원 가입 실패 - 닉네임 중복")
	void createMember_fail_duplicateNickname() {
		// given
		String email = "user2@example.com";
		String nickname = "dupnick";
		given(memberRepository.existsByEmail(email)).willReturn(false);
		given(memberRepository.existsByNickname(nickname)).willReturn(true);

		// when & then
		ServiceException ex = catchThrowableOfType(
			() -> memberService.createMember(
				new MemberSignUpRequest(email, "pw", nickname, LocalDate.now())),
			ServiceException.class);

		assertThat(ex.getCode()).isEqualTo("409-2");
		assertThat(ex.getMessage()).contains("이미 사용중인 닉네임입니다.");
	}

	//--- 로그인 테스트 --------------------------------------

	@Test
	@DisplayName("로그인 성공")
	void loginMember_success() {
		// given
		String email = "login@example.com";
		String rawPassword = "secret";
		Member member = Member.builder()
			.id(UUID.randomUUID())
			.email(email)
			.passwordHash("encoded")
			.nickname("nick")
			.role(MemberRole.USER)
			.build();

		given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
		given(passwordEncoder.matches(rawPassword, member.getPasswordHash())).willReturn(true);
		given(jwtUtils.createToken(anyMap())).willReturn("jwt-token");

		// when
		MemberLoginResponse loginResp = memberService.loginMember(email, rawPassword);

		// then
		assertThat(loginResp.email()).isEqualTo(email);
		assertThat(loginResp.nickname()).isEqualTo("nick");
		assertThat(loginResp.token()).isEqualTo("jwt-token");
	}

	@Test
	@DisplayName("로그인 실패 - 이메일 없음")
	void loginMember_fail_notFoundEmail() {
		// given
		given(memberRepository.findByEmail("nope@example.com")).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> memberService.loginMember("nope@example.com", "any"))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("존재하지 않는 이메일입니다.");
	}

	@Test
	@DisplayName("로그인 실패 - 비밀번호 불일치")
	void loginMember_fail_badPassword() {
		// given
		String email = "user@example.com";
		Member member = Member.builder()
			.email(email)
			.passwordHash("hash")
			.build();

		given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
		given(passwordEncoder.matches("wrong", member.getPasswordHash())).willReturn(false);

		// when & then
		assertThatThrownBy(() -> memberService.loginMember(email, "wrong"))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("비밀번호가 올바르지 않습니다.");
	}
}
