package site.kkokkio.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4);             // 스레드 풀에 항상 살아있는 최소 스레드 수
        executor.setMaxPoolSize(8);              // 스레드 풀이 확장할 수 있는 최대 스레드 수
        executor.setQueueCapacity(100);          // 스레드 풀에서 사용할 최대 큐의 크기
        executor.setThreadNamePrefix("KK-Async-"); // 생성된 각 스레드의 이름 접두사

        // 큐도 꽉 차면 Caller 쓰레드에서 실행시킴 (fallback)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}