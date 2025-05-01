package site.kkokkio.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.repository.MemberRepository;
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
}
