package site.kkokkio.domain.batch.step;

import static site.kkokkio.domain.batch.context.ExecutionContextKeys.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.batch.core.Step;
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
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.service.TrendsService;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FetchTrendingKeywordsStepConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final TrendsService trendsService;
	private final LogStepListener err;
	private final BatchMetricsListener metrics;

	@Bean
	public Step fetchTrendingKeywordsStep() {
		return new StepBuilder("fetchTrendingKeywordsStep", jobRepository)
			.tasklet((contrib, ctx) -> {

				ExecutionContext jobEc = ctx.getStepContext()
					.getStepExecution()
					.getJobExecution()
					.getExecutionContext();
				ExecutionContext stepEc = ctx.getStepContext().getStepExecution().getExecutionContext();

				// bucketAt : JobParameter 로 넘어왔으면 재사용, 없으면 생성
				LocalDateTime bucketAt = jobEc.containsKey(EC_BUCKET_AT)
					? (LocalDateTime)jobEc.get(EC_BUCKET_AT)
					: LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

				if (!jobEc.containsKey(EC_BUCKET_AT)) {
					jobEc.put(EC_BUCKET_AT, bucketAt);
				}

				// Google Trends 호출 + 지연(ms) 측정
				long t0 = System.currentTimeMillis();
				List<Keyword> keywords = trendsService.getTrendingKeywordsFromRss();
				long latency = System.currentTimeMillis() - t0;

				// JobExecutionContext 업데이트
				List<Long> keywordIds = keywords.stream().map(Keyword::getId).toList();
				jobEc.put(EC_TOP_IDS, keywordIds);
				jobEc.putInt(EC_TOP_COUNT, keywordIds.size());

				// StepExecutionContext 업데이트
				stepEc.putLong(SC_RSS_LATENCY_MS, latency);

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.listener(metrics)
			.listener(err)
			.allowStartIfComplete(true)
			.build();
	}
}
