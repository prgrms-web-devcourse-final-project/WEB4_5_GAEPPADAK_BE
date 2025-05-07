package site.kkokkio.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import site.kkokkio.global.listener.RedisExpiredKeyListener;

@Configuration
public class RedisListenerConfig {

	// 만료된 Redis 키 이벤트에 반응하는 RedisMessageListenerContainer 빈
	@Bean
	public RedisMessageListenerContainer keyExpirationListenerContainer(
		RedisConnectionFactory cf,
		RedisExpiredKeyListener expiredListener) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		// RedisConnectionFactory 설정
		container.setConnectionFactory(cf);
		// expired 이벤트 패턴 구독
		container.addMessageListener(
			expiredListener,
			new PatternTopic("__keyevent@*__:expired"));
		return container;
	}
}
