package site.kkokkio.domain.member.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.controller.dto.MemberUpdateRequest;
import site.kkokkio.domain.member.controller.dto.PasswordResetRequest;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.repository.MemberRepository;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.ServiceException;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final RedisTemplate<String, String> redisTemplate;

	// 회원 가입
	@Transactional
	public MemberResponse createMember(MemberSignUpRequest request) {

		// 중복 검사
		if (memberRepository.existsByEmail(request.email())) {
			throw new ServiceException("409", "이미 사용중인 이메일입니다.");
		}
		if (memberRepository.existsByNickname(request.nickname())) {
			throw new ServiceException("409", "이미 사용중인 닉네임입니다.");
		}

		// 비밀번호 암호화
		String encryptedPassword = passwordEncoder.encode(request.passwordHash());

		// 회원 생성
		Member member = Member.builder()
			.email(request.email())
			.passwordHash(encryptedPassword)
			.nickname(request.nickname())
			.birthDate(request.birthDate())
			.role(MemberRole.USER)// 기본적으로 USER 권한 부여
			.build();
		memberRepository.save(member);
		return new MemberResponse(member);
	}

	// // Redis에 가입 할 회원 ID 저장
	public void siginUpUnverified(MemberResponse response) {
		// Redis에 가입 할 회원 ID 저장
		String redisKey = "SIGNUP_UNVERIFIED:" + response.getEmail();
		redisTemplate.opsForValue().set(redisKey, "1", Duration.ofMinutes(5)); // 5분
	}

	// 이메일로 회원 조회
	// (로그인, 토큰 재발급 등에서 사용)
	public Member findByEmail(String email) {
		return memberRepository.findByEmail(email)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 이메일입니다."));
	}

	// 회원 정보 조회
	public MemberResponse getMemberInfo(String email) {
		// 멤버 조회 및 응답 DTO 변환
		Member memberInfo = findByEmail(email);
		return new MemberResponse(memberInfo);
	}

	// 회원 정보 수정
	@Transactional
	public MemberResponse modifyMemberInfo(CustomUserDetails userDetails, MemberUpdateRequest requestBody) {

		Member member = findByEmail(userDetails.getUsername());

		String newNickname = requestBody.nickname();
		String oldNickname = member.getNickname();
		String newPassword = requestBody.password();
		String oldPassword = member.getPasswordHash();

		duplicationCheck(newNickname, oldNickname, newPassword, oldPassword);

		// 회원 정보 수정 - 닉네임, 비밀번호가 입력되지 않을 경우 기존을 유지
		Member modifiedMember = Member.builder()
			.id(member.getId())
			.email(member.getEmail())
			.nickname(newNickname != null ? newNickname : oldNickname)
			.birthDate(member.getBirthDate())
			.passwordHash(newPassword != null ? passwordEncoder.encode(newPassword) :
				oldPassword)
			.role(member.getRole())
			.emailVerified(member.isEmailVerified())
			.build();

		memberRepository.save(modifiedMember);

		return new MemberResponse(modifiedMember);
	}

	// 중복 확인 로직
	public void duplicationCheck(String newNickname, String oldNickname, String newPassword, String oldPassword) {
		if (newNickname != null && !newNickname.isEmpty()) { // 닉네임 변경 요청이 있을 경우에만 검증
			if (newNickname.equals(oldNickname)) {
				// 현재 닉네임과 동일한 닉네임으로 변경 시도
				throw new ServiceException("400", "현재 닉네임과 동일한 닉네임입니다.");
			}
			if (memberRepository.existsByNickname(newNickname)) {
				// 이미 존재하는 닉네임일 경우
				throw new ServiceException("409", "이미 사용중인 닉네임입니다."); // 409 Conflict
			}
		}

		if (newPassword != null && !newPassword.isEmpty()) { // 비밀번호 변경 요청이 있을 경우에만 검증
			// 현재 비밀번호와 동일한지 확인 (인코딩된 비밀번호 비교)
			if (passwordEncoder.matches(newPassword, oldPassword)) {
				throw new ServiceException("400", "현재 비밀번호와 동일한 비밀번호입니다.");
			}
		}
	}



	// 비밀번호 초기화
	@Transactional
	public void resetPassword(PasswordResetRequest request) {

		String key = "EMAIL_VERIFIED:" + request.email();

		// 인증 여부 확인
		String verified = redisTemplate.opsForValue().get(key);
		if (!"true".equals(verified)) {
			throw new ServiceException("401", "인증코드가 유효하지 않습니다.");
		}

		// 회원 조회
		Member member = findByEmail(request.email());

		// 비밀번호 암호화 및 저장
		String encryptedPassword = passwordEncoder.encode(request.newPassword());
		member.setPasswordHash(encryptedPassword);
		memberRepository.save(member);

		// Redis 인증 플래그 삭제
		redisTemplate.delete(key);
	}

	@Transactional
	public void deleteMember(UserDetails userDetails) {
		Member member = findByEmail(userDetails.getUsername());
		member.maskPersonalInfo();
		member.softDelete();
		memberRepository.save(member);
	}

	/**
	 * 관리자용 회원 목록 조회
	 * @param pageable 페이징 및 정렬 정보
	 * @param searchTarget 검색 대상 필드
	 * @param searchValue 검색어
	 * @return 페이징된 회원 목록
	 */
	@Transactional
	public Page<Member> getAdminMemberList(Pageable pageable, String searchTarget, String searchValue) {
		// 검색
		if (searchTarget != null && searchValue != null && !searchValue.trim().isEmpty()) {
			String trimmedSearchTarget = searchTarget.trim();
			String trimmedSearchValue = searchValue.trim();

			// 공백 제거 후 searchTarget이 비어있지 않다면 검색 로직 계속 진행
			if (!trimmedSearchTarget.isEmpty()) {

				// 검색 대상에 따라 적절한 Repository 메서드 호출
				switch (trimmedSearchTarget.toLowerCase()) {
					// 닉네임으로 검색
					case "nickname":
						return memberRepository
							.findByNicknameContainingIgnoreCase(trimmedSearchValue, pageable);

					// 이메일로 검색
					case "email":
						return memberRepository
							.findByEmailContainingIgnoreCase(trimmedSearchValue, pageable);

					// 역할로 검색
					case "role":
						try {
							// 대소문자 구분 없이 입력된 문자열을 MemberRole Enum으로 변환 시도
							MemberRole role = MemberRole.valueOf(trimmedSearchValue.toUpperCase());

							return memberRepository.findByRole(role, pageable);
						} catch (IllegalArgumentException e) {
							// 유효하지 않은 역할 문자열이 입력된 경우 빈 객체 반환
							return Page.empty(pageable);
						}

						// 지원하지 않는 검색 대상일 경우 전체 조회
					default:
						break;
				}
			}
		}

		// 검색 조건이 없거나 지원하지 않는 검색 대상일 경우 전체 회원 조회
		return memberRepository.findAll(pageable);
	}

	/**
	 * 관리자용 회원 역할 변경
	 * @param memberId 역할을 변경할 회원 ID
	 * @param newRole 변경할 역할 (USER 또는 BLACK)
	 * @return 업데이트된 Member 엔티티
	 */
	@Transactional
	public Member updateMemberRole(UUID memberId, MemberRole newRole) {

		// 1. memberId로 회원 조회
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 사용자입니다."));

		// 2. 조회된 회원이 'ADMIN' 역할인지 확인
		if (member.getRole() == MemberRole.ADMIN) {
			throw new ServiceException("400", "관리자 역할은 변경할 수 없습니다.");
		}

		// 3. 변경할 역할이 ADMIN인지 확인
		if (newRole == MemberRole.ADMIN) {
			throw new ServiceException("400", "관리자 역할은 부여할 수 없습니다.");
		}

		// 4. 회원의 역할을 newRole로 변경
		member.setRole(newRole);

		return member;
	}
}
