package site.kkokkio.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.repository.MemberRepository;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.util.JwtUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceV2Test {

	@InjectMocks
	private MemberService memberService;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtils jwtUtils;

	private final UUID testMemberId = UUID.randomUUID();
	private final UUID adminMemberId = UUID.randomUUID();
	private final UUID blackMemberId = UUID.randomUUID();

	private final Member sampleUserMember = Member.builder()
		.id(testMemberId)
		.email("test@test.com")
		.passwordHash("testPassword")
		.nickname("testUser")
		.birthDate(LocalDate.of(1990, 1, 1))
		.role(MemberRole.USER)
		.emailVerified(true)
		.build();

	private final Member sampleAdminMember = Member.builder()
		.id(adminMemberId)
		.email("admin@test.com")
		.passwordHash("adminPassword")
		.nickname("adminUser")
		.birthDate(LocalDate.of(1980, 2, 2))
		.role(MemberRole.ADMIN)
		.emailVerified(true)
		.build();

	private final Member sampleBlackMember = Member.builder()
		.id(blackMemberId)
		.email("black@test.com")
		.passwordHash("blackPassword")
		.nickname("blackUser")
		.birthDate(LocalDate.of(2000, 3, 3))
		.role(MemberRole.BLACK)
		.emailVerified(true)
		.build();

	@Test
	@DisplayName("검색 조건 없이 회원 목록 조회")
	void getAdminMemberList_NoSearch() {
		/// given
		// Repository가 반환할 가상 회원 목록 및 Page 객체 생성
		List<Member> memberList = Arrays.asList(sampleAdminMember, sampleUserMember, sampleBlackMember);
		Pageable pageable = PageRequest.of(0, 10, Sort.by("nickname").ascending());
		Page<Member> memberPage = new PageImpl<>(memberList, pageable, memberList.size());

		given(memberRepository.findAll(any(Pageable.class))).willReturn(memberPage);

		/// when
		Page<Member> resultPage = memberService.getAdminMemberList(pageable, null, null);

		/// then
		assertThat(resultPage).isNotNull();
		assertThat(resultPage.getContent()).hasSize(3);
		assertThat(resultPage.getTotalElements()).isEqualTo(3);
		assertThat(resultPage.getNumber()).isEqualTo(0);

		then(memberRepository).should(times(1)).findAll(eq(pageable));
		then(memberRepository).should(never()).findByNicknameContainingIgnoreCase(any(), any());
		then(memberRepository).should(never()).findByEmailContainingIgnoreCase(any(), any());
		then(memberRepository).should(never()).findByRole(any(), any());
	}

	@Test
	@DisplayName("닉네임 검색")
	void getAdminMemberList_SearchByNickname() {
		/// given
		// Repository가 반환할 가상 회원 목록 및 Page 객체 생성
		String searchValue = "testUser";
		Pageable pageable = PageRequest.of(0, 10, Sort.by("nickname").ascending());
		List<Member> searchedList = Arrays.asList(sampleUserMember);
		Page<Member> searchedPage = new PageImpl<>(searchedList, pageable, searchedList.size());

		given(memberRepository.findByNicknameContainingIgnoreCase(
			eq(searchValue.trim()), eq(pageable))).willReturn(searchedPage);

		/// when
		Page<Member> resultPage = memberService.getAdminMemberList(pageable, "nickname", searchValue);

		/// then
		assertThat(resultPage).isNotNull();
		assertThat(resultPage.getContent()).hasSize(1);
		assertThat(resultPage.getContent().getFirst().getNickname()).isEqualTo("testUser");

		then(memberRepository).should(times(1))
			.findByNicknameContainingIgnoreCase(eq(searchValue.trim()), eq(pageable));
		then(memberRepository).should(never()).findAll(any(Pageable.class));
		then(memberRepository).should(never()).findByEmailContainingIgnoreCase(any(), any());
		then(memberRepository).should(never()).findByRole(any(), any());
	}

	@Test
	@DisplayName("이메일 검색")
	void getAdminMemberList_SearchByEmail() {
		/// given
		// Repository가 반환할 가상 회원 목록 및 Page 객체 생성
		String searchValue = "admin@test.com";
		Pageable pageable = PageRequest.of(0, 10, Sort.by("nickname").ascending());
		List<Member> searchedList = Arrays.asList(sampleAdminMember);
		Page<Member> searchedPage = new PageImpl<>(searchedList, pageable, searchedList.size());

		given(memberRepository.findByEmailContainingIgnoreCase(
			eq(searchValue.trim()), eq(pageable))).willReturn(searchedPage);

		/// when
		Page<Member> resultPage = memberService.getAdminMemberList(pageable, "email", searchValue);

		/// then
		assertThat(resultPage).isNotNull();
		assertThat(resultPage.getContent()).hasSize(1);
		assertThat(resultPage.getContent().getFirst().getEmail()).isEqualTo("admin@test.com");

		then(memberRepository).should(times(1))
			.findByEmailContainingIgnoreCase(eq(searchValue.trim()), eq(pageable));
		then(memberRepository).should(never()).findAll(any(Pageable.class));
		then(memberRepository).should(never()).findByNicknameContainingIgnoreCase(any(), any());
		then(memberRepository).should(never()).findByRole(any(), any());
	}

	@Test
	@DisplayName("역할 검색")
	void getAdminMemberList_SearchByRole() {
		/// given
		// Repository가 반환할 가상 회원 목록 및 Page 객체 생성
		String searchValue = "BLACK";
		Pageable pageable = PageRequest.of(0, 10, Sort.by("nickname").ascending());
		List<Member> searchedList = Arrays.asList(sampleBlackMember);
		Page<Member> searchedPage = new PageImpl<>(searchedList, pageable, searchedList.size());

		given(memberRepository.findByRole(
			eq(MemberRole.BLACK), eq(pageable))).willReturn(searchedPage);

		/// when
		Page<Member> resultPage = memberService.getAdminMemberList(pageable, "role", searchValue);

		/// then
		assertThat(resultPage).isNotNull();
		assertThat(resultPage.getContent()).hasSize(1);
		assertThat(resultPage.getContent().getFirst().getRole()).isEqualTo(MemberRole.BLACK);

		then(memberRepository).should(times(1))
			.findByRole(eq(MemberRole.BLACK), eq(pageable));
		then(memberRepository).should(never()).findAll(any(Pageable.class));
		then(memberRepository).should(never()).findByNicknameContainingIgnoreCase(any(), any());
		then(memberRepository).should(never()).findByEmailContainingIgnoreCase(any(), any());
	}

	@Test
	@DisplayName("유효하지 않은 역할 문자열로 검색 시 빈 결과 반환")
	void getAdminMemberList_SearchByInvalidRoleString() {
		/// given
		// Repository가 반환할 가상 회원 목록 및 Page 객체 생성
		String searchValue = "INVALID_ROLE";
		Pageable pageable = PageRequest.of(0, 10, Sort.by("nickname").ascending());

		/// when
		Page<Member> resultPage = memberService.getAdminMemberList(pageable, "role", searchValue);

		/// then
		assertThat(resultPage).isNotNull();
		assertThat(resultPage.isEmpty()).isTrue();

		then(memberRepository).should(never()).findAll(any(Pageable.class));
		then(memberRepository).should(never()).findByNicknameContainingIgnoreCase(any(), any());
		then(memberRepository).should(never()).findByEmailContainingIgnoreCase(any(), any());
	}

	@Test
	@DisplayName("역할 변경 성공 (BLACK -> USER)")
	void updateMemberRole_Success_BlackToUser() {
		/// given
		given(memberRepository.findById(blackMemberId)).willReturn(Optional.of(sampleBlackMember));

		/// when
		Member updatedMember = memberService.updateMemberRole(blackMemberId, MemberRole.USER);

		/// then
		assertThat(updatedMember).isNotNull();
		assertThat(updatedMember.getId()).isEqualTo(blackMemberId);
		assertThat(updatedMember.getRole()).isEqualTo(MemberRole.USER);

		then(memberRepository).should(times(1)).findById(blackMemberId);
	}

	@Test
	@DisplayName("존재하지 않는 회원 역할 변경")
	void updateMemberRole_Fail_MemberNotFound() {
		/// given
		UUID nonExistentId = UUID.randomUUID();
		given(memberRepository.findById(nonExistentId)).willReturn(Optional.empty());

		/// when & then
		assertThatThrownBy(() -> memberService.updateMemberRole(nonExistentId, MemberRole.BLACK))
			.isInstanceOf(ServiceException.class)
			.hasFieldOrPropertyWithValue("code", "404")
			.hasMessageContaining("존재하지 않는 사용자입니다.");

		then(memberRepository).should(times(1)).findById(eq(nonExistentId));
		then(memberRepository).should(never()).save(any(Member.class));
	}

	@Test
	@DisplayName("대상 회원이 ADMIN일 경우")
	void updateMemberRole_Fail_TargetIsAdmin() {
		/// given
		given(memberRepository.findById(adminMemberId)).willReturn(Optional.of(sampleAdminMember));

		/// when & then
		assertThatThrownBy(() -> memberService.updateMemberRole(adminMemberId, MemberRole.BLACK))
			.isInstanceOf(ServiceException.class)
			.hasFieldOrPropertyWithValue("code", "400")
			.hasMessageContaining("관리자 역할은 변경할 수 없습니다.");

		then(memberRepository).should(times(1)).findById(eq(adminMemberId));
		then(memberRepository).should(never()).save(any(Member.class));
	}

	@Test
	@DisplayName("ADMIN 역할 부여 시도")
	void updateMemberRole_Fail_AssignAdminRole() {
		/// given
		given(memberRepository.findById(testMemberId)).willReturn(Optional.of(sampleUserMember));

		/// when & then
		assertThatThrownBy(() -> memberService.updateMemberRole(testMemberId, MemberRole.ADMIN))
			.isInstanceOf(ServiceException.class)
			.hasFieldOrPropertyWithValue("code", "400")
			.hasMessageContaining("관리자 역할은 부여할 수 없습니다.");

		then(memberRepository).should(times(1)).findById(eq(testMemberId));
		then(memberRepository).should(never()).save(any(Member.class));
	}
}
