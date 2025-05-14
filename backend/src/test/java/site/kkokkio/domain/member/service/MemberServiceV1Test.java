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
import org.springframework.security.crypto.password.PasswordEncoder;

import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.controller.dto.MemberUpdateRequest;
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

		CustomUserDetails userDetails = new CustomUserDetails(member);

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
	@DisplayName("회원 탈퇴 성공")
	void deleteMember_success() {
		// given
		Member member = Mockito.spy(Member.builder()
			.id(UUID.randomUUID())
			.email("test@example.com")
			.nickname("사용자")
			.build());

		// when
		memberService.deleteMember(member);

		// then
		verify(member).maskPersonalInfo();
		verify(member).softDelete();
		verify(memberRepository).save(member);
	}
}
