package site.kkokkio.global.listener;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.member.repository.MemberRepository;

@Component
@RequiredArgsConstructor
public class RedisExpiredKeyListener implements MessageListener {
	private final MemberRepository memberRepository;

	// 만료된 Redis 키가 넘어올 때마다 호출되는 콜백 메서드
	@Override
	public void onMessage(Message msg, byte[] pattern) {
		String key = msg.toString();

		// "SIGNUP_UNVERIFIED:"로 시작되는 키만 확인
		if (!key.startsWith("SIGNUP_UNVERIFIED:"))
			return;

		// key에서 email값 분리
		String email = key.split(":", 2)[1];

		// 메일 인증이 안된 회원 삭제
		memberRepository.findByEmail(email).ifPresent(m -> {
			if (!m.isEmailVerified()) {
				memberRepository.delete(m);
			}
		});
	}
}
