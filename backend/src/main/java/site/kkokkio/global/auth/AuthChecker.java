package site.kkokkio.global.auth;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.comment.repository.CommentRepository;
import site.kkokkio.domain.member.entity.Member;

@Component("authChecker")
@RequiredArgsConstructor
public class AuthChecker {

	private final CommentRepository commentRepository;

	public boolean isSelf(Authentication authentication) {
		return authentication != null && authentication.isAuthenticated();
	}

	public boolean isOwner(String resource, Long id, Authentication auth) {

		if (auth == null || id == null || !auth.isAuthenticated()) {
			return false;
		}

		Member currentUser = ((CustomUserDetails)auth.getPrincipal()).getMember();

		return switch (resource) {
			case "comment" -> commentRepository.findById(id)
				.filter(comment -> comment.getMember().getId().equals(currentUser.getId()))
				.isPresent();
			default -> false;
		};
	}
}
