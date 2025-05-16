package site.kkokkio.domain.batch.step;

import static site.kkokkio.domain.batch.context.BatchConstants.*;
import static site.kkokkio.domain.batch.context.ExecutionContextKeys.*;

import java.time.Duration;
import java.util.List;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.batch.listener.BatchMetricsListener;
import site.kkokkio.domain.batch.listener.LogStepListener;
import site.kkokkio.domain.post.service.PostService;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CachePostStepConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final PostService postService;
	private final LogStepListener err;
	private final BatchMetricsListener metrics;

	@Bean(name = CACHE_POST_STEP)
	public Step cachePostStep() {
		return new StepBuilder(CACHE_POST_STEP, jobRepository)
			.tasklet((contrib, ctx) -> {

				StepExecution se = ctx.getStepContext().getStepExecution();
				ExecutionContext jobEc = se.getJobExecution().getExecutionContext();
				ExecutionContext stepEc = se.getExecutionContext();

				@SuppressWarnings("unchecked")
				List<Long> newPostIds =
					(List<Long>)se.getJobExecution().getExecutionContext().get(JC_NEW_POST_IDS);
				@SuppressWarnings("unchecked")
				List<Long> keywordIds = (List<Long>)jobEc.get(JC_POSTABLE_IDS);

				// 캐싱이 필요 없는 경우 빠른 종료
				if (newPostIds.isEmpty()) {
					stepEc.putInt(SC_CACHE_SIZE, 0);
					return RepeatStatus.FINISHED;
				}

				// Redis 캐싱 (24h TTL)
				int cached = postService.cacheCardViews(newPostIds, keywordIds, Duration.ofHours(24));

				// StepExecutionContext 업데이트
				stepEc.putInt(SC_CACHE_SIZE, cached);  // Gauge (batch_cache_size)

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.listener(err)
			.listener(metrics)
			.build();
	}
}
