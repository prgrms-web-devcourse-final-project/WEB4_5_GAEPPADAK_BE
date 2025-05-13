package site.kkokkio.domain.member.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.controller.dto.MemberResponse;
import site.kkokkio.domain.member.controller.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.controller.dto.MemberUpdateRequest;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.repository.MemberRepository;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.exception.CustomAuthException;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.util.JwtUtils;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtils jwtUtils;
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
		redisTemplate.opsForValue()
			.set(redisKey, "1", Duration.ofMinutes(5)); // 5분
	}

	// 이메일로 회원 조회
	// (로그인, 토큰 재발급 등에서 사용)
	public Member findByEmail(String email) {
		return memberRepository.findByEmail(email)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 이메일입니다."));
	}

	// // 로그인
	// public MemberLoginResponse loginMember(String email, String password) {
	//
	// 	// 회원 이메일 확인
	// 	Member member = memberRepository.findByEmail(email).orElseThrow(() ->
	// 		new ServiceException("404", "존재하지 않는 이메일입니다."));
	//
	// 	// 비밀번호 확인
	// 	if (!passwordEncoder.matches(password, member.getPasswordHash())) {
	// 		throw new ServiceException("401", "비밀번호가 올바르지 않습니다.");
	// 	}
	//
	// 	// JWT 토큰 발생 시 포함할 사용자 정보 설정
	// 	Map<String, Object> claims = new HashMap<>();
	// 	claims.put("commentId", member.getId());
	// 	claims.put("email", member.getEmail());
	// 	claims.put("nickname", member.getNickname());
	// 	claims.put("role", member.getRole());
	// 	String token = jwtUtils.createToken(claims); // 토큰 생성
	//
	// 	return MemberLoginResponse.of(member, token,);
	// }

	// 회원 정보 조회
	public MemberResponse getMemberInfo(HttpServletRequest request) {
		// 쿠키에서 access-token 추출
		String token = jwtUtils.getJwtFromCookies(request)
			.orElseThrow(() -> new CustomAuthException(
				CustomAuthException.AuthErrorType.MISSING_TOKEN));

		// 토큰 검증
		jwtUtils.isValidToken(token);
		// 페이로드에서 사용자 이메일 추출
		Claims claims = jwtUtils.getPayload(token);

		// claims.subject 유효성 검사
		String email = claims.getSubject();
		if (email == null || email.isBlank()) {
			throw new CustomAuthException(CustomAuthException.AuthErrorType.MALFORMED_TOKEN);
		}

		// 멤버 조회 및 응답 DTO 변환
		Member memberInfo = findByEmail(email);
		return new MemberResponse(memberInfo);
	}

	// 회원 정보 수정
	public MemberResponse modifyMemberInfo(HttpServletRequest request, MemberUpdateRequest requestBody) {
		// 쿠키에서 access-token 추출
		String token = jwtUtils.getJwtFromCookies(request)
			.orElseThrow(() -> new CustomAuthException(
				CustomAuthException.AuthErrorType.MISSING_TOKEN));

		// 토큰 검증
		jwtUtils.isValidToken(token);
		// 페이로드에서 사용자 이메일 추출
		Claims claims = jwtUtils.getPayload(token);

		// claims.subject 유효성 검사
		String email = claims.getSubject();
		if (email == null || email.isBlank()) {
			throw new CustomAuthException(CustomAuthException.AuthErrorType.MALFORMED_TOKEN);
		}

		Member member = findByEmail(email);

		// 회원 정보 수정
		Member modifiedMember = Member.builder()
			.id(member.getId())
			.email(member.getEmail())
			.nickname(requestBody.nickname() != null ? requestBody.nickname() : member.getNickname())
			.birthDate(member.getBirthDate()) // 기존 생년월일 유지
			.passwordHash(requestBody.passwordHash() != null ? passwordEncoder.encode(requestBody.passwordHash()) : member.getPasswordHash())
			.role(member.getRole())
			.emailVerified(member.isEmailVerified())
			.build(); // 기존 역할 유지

		memberRepository.save(modifiedMember);

		return new MemberResponse(modifiedMember);
	}
}
