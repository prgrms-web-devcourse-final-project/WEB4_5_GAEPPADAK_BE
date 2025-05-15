package site.kkokkio.domain.batch;

import static site.kkokkio.domain.batch.context.BatchConstants.*;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.batch.listener.BatchMetricsListener;

@Configuration
@RequiredArgsConstructor
public class TrendToPostJobConfig {

	private final JobRepository jobRepository;
	private final BatchMetricsListener metrics;
	private final Step fetchTrendingKeywordsStep;
	private final Flow searchSourcesFlow;
	private final Step evaluateNoveltyStep;
	private final JobExecutionDecider noveltyDecider;
	private final Step generatePostStep;
	private final Step cachePostStep;

	/* === Job 정의 === */
	@Bean
	public Job trendToPostJob() {
		Flow trendToPostFlow = new FlowBuilder<SimpleFlow>(TREND_TO_POST_JOB)
			.start(fetchTrendingKeywordsStep)
			.next(searchSourcesFlow)
			.next(evaluateNoveltyStep)
			.next(noveltyDecider)
			.on(NO_POST_NEEDED_STATUS).to(cachePostStep)
			.from(noveltyDecider).on("*").to(generatePostStep)
			.next(cachePostStep)
			.end();

		return new JobBuilder(TREND_TO_POST_JOB, jobRepository)
			.incrementer(new RunIdIncrementer())
			.listener(metrics)
			.start(trendToPostFlow)
			.end()
			.build();
	}
}