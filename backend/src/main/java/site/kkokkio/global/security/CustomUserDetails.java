// 파일: src/main/java/site/kkokkio/global/security/CustomUserDetails.java
package site.kkokkio.global.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import site.kkokkio.domain.member.entity.Member;

public class CustomUserDetails implements UserDetails {
	private final Member member;

	public CustomUserDetails(Member member) {
		this.member = member;
	}

	/** 도메인 Member 객체를 외부에서 꺼내 쓸 때 사용합니다. */
	public Member getMember() {
		return member;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return AuthorityUtils.createAuthorityList(member.getRole().name());
	}

	@Override
	public String getPassword() {
		return member.getPasswordHash();
	}

	@Override
	public String getUsername() {
		return member.getEmail();
	}

	@Override
	public boolean isEnabled() {
		return member.isEmailVerified();
	}
}