package site.kkokkio.domain.batch.step;

import static site.kkokkio.domain.batch.context.BatchConstants.*;
import static site.kkokkio.domain.batch.context.ExecutionContextKeys.*;

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
import site.kkokkio.domain.source.dto.SearchStatsDto;
import site.kkokkio.domain.source.service.SourceService;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SearchVideosStepConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final SourceService sourceService;
	private final LogStepListener err;
	private final BatchMetricsListener metrics;

	@Bean(name = SEARCH_VIDEOS_STEP)
	public Step searchNewsStep() {
		return new StepBuilder(SEARCH_VIDEOS_STEP, jobRepository)
			.tasklet((contrib, ctx) -> {

				ExecutionContext stepEc = ctx.getStepContext()
					.getStepExecution()
					.getExecutionContext();

				SearchStatsDto stat = sourceService.searchYoutube();

				stepEc.putInt(SC_VIDEO_FETCHED, stat.fetched());
				stepEc.putInt(SC_VIDEO_API_FAIL, stat.failed());

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.listener(err)
			.listener(metrics)
			.allowStartIfComplete(true)
			.build();
	}
}
