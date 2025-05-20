package site.kkokkio.global.auth;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import site.kkokkio.global.enums.MemberRole;

public class CustomUserDetails implements UserDetails {
	private final String email;
	private final String role;
	private final Boolean isVerified;

	public CustomUserDetails(String email, String role, Boolean isVerified) {
		this.email = email;
		this.role = role;
		this.isVerified = isVerified;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return AuthorityUtils.createAuthorityList("ROLE_" + role);
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return isVerified;
	}

	@Override
	public boolean isAccountNonExpired() {
		// 계정 만료 로직을 구현할 수 있으나, 현재는 항상 만료되지 않음으로 처리
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// BLACK 역할인 경우 잠금 처리
		return !MemberRole.BLACK.toString().equals(role);
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// 자격 증명(비밀번호) 만료 로직을 구현할 수 있으나, 현재는 항상 만료되지 않음으로 처리
		return true;
	}
}
