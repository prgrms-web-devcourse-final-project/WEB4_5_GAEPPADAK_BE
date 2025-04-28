package site.kkokkio.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.dto.MemberResponse;
import site.kkokkio.domain.member.dto.MemberSignUpRequest;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.repository.MemberRepository;
import site.kkokkio.global.enums.MemberRole;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	public MemberResponse createMember(MemberSignUpRequest request) {

		// 중복 검사
		if (memberRepository.existsByEmail(request.email())) {
			throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
		}
		if (memberRepository.existsByNickname(request.nickname())) {
			throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
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
}
