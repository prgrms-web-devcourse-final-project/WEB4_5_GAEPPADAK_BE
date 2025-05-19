package site.kkokkio.domain.batch.step;

import static site.kkokkio.domain.batch.context.BatchConstants.*;
import static site.kkokkio.domain.batch.context.ExecutionContextKeys.*;

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

	@Bean(name = FETCH_KEYWORDS_STEP)
	public Step fetchTrendingKeywordsStep() {
		return new StepBuilder(FETCH_KEYWORDS_STEP, jobRepository)
			.tasklet((contrib, ctx) -> {

				ExecutionContext jobEc = ctx.getStepContext()
					.getStepExecution()
					.getJobExecution()
					.getExecutionContext();

				List<Keyword> keywords = trendsService.getTrendingKeywordsFromRss();

				// Update JobExecutionContext
				List<Long> keywordIds = keywords.stream().map(Keyword::getId).toList();
				jobEc.put(JC_TOP_KEYWORD_IDS, keywordIds);
				jobEc.putInt(JC_TOP_KEYWORD_COUNT, keywordIds.size());

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.listener(metrics)
			.listener(err)
			.allowStartIfComplete(true)
			.build();
	}
}
