package site.kkokkio.domain.batch.step;

import static site.kkokkio.domain.batch.context.BatchConstants.*;
import static site.kkokkio.domain.batch.context.ExecutionContextKeys.*;

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
import site.kkokkio.domain.keyword.dto.NoveltyStatsDto;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EvaluateNoveltyStepConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final KeywordMetricHourlyService keywordMetricHourlyService;
	private final LogStepListener err;
	private final BatchMetricsListener metrics;

	@Bean(name = EVALUATE_NOVELTY_STEP)
	public Step evalutateNoveltyStep() {
		return new StepBuilder(EVALUATE_NOVELTY_STEP, jobRepository)
			.tasklet((contrib, ctx) -> {

				StepExecution se = ctx.getStepContext().getStepExecution();
				ExecutionContext jobEc = se.getJobExecution().getExecutionContext();
				ExecutionContext stepEc = se.getExecutionContext();

				// Top-10 키워드 ID 가져오기
				@SuppressWarnings("unchecked")
				List<Long> topKeywordIds = (List<Long>)jobEc.get(JC_TOP_IDS);

				// 1) Novelty 계산 & keyword_metric_hourly UPDATE
				NoveltyStatsDto ns = keywordMetricHourlyService.evaluateNovelty(topKeywordIds);
				int lowVarCnt = ns.lowVariationCount();

				// 2) low_variation = false 인 키워드만 다음 Step 전달
				jobEc.put(JC_POSTABLE_IDS, ns.postableIds());
				jobEc.putInt(JC_POSTABLE_COUNT, ns.postableIds().size());

				// 3) StepExecutionContext 업데이트
				stepEc.putInt(SC_NOVELTY_SKIPPED, lowVarCnt);   // Counter

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.listener(err)
			.listener(metrics)
			.build();
	}
}
