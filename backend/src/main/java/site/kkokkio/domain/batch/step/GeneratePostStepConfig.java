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
import site.kkokkio.domain.post.service.PostService;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GeneratePostStepConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final PostService postService;
	private final LogStepListener err;
	private final BatchMetricsListener metrics;

	@Bean(name = GENERATE_POST_STEP)
	public Step generatePostStep() {
		return new StepBuilder(GENERATE_POST_STEP, jobRepository)
			.tasklet((contrib, ctx) -> {

				StepExecution se = ctx.getStepContext().getStepExecution();
				ExecutionContext jobEc = se.getJobExecution().getExecutionContext();
				ExecutionContext stepEc = se.getExecutionContext();

				// 대상 키워드(id) 목록
				@SuppressWarnings("unchecked")
				List<Long> ids = (List<Long>)jobEc.get(JC_POSTABLE_KEYWORD_IDS);

				// Post 생성 (신규 Source ↔ LLM 요약)
				List<Long> newPostIds = postService.generatePosts(ids);
				int created = ids.isEmpty() ? 0 : newPostIds.size();

				// StepExecutionContext 업데이트
				jobEc.put(JC_NEW_POST_IDS, newPostIds);
				stepEc.putInt(SC_POST_CREATED, created);          // Counter

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.listener(err)
			.listener(metrics)
			.allowStartIfComplete(true)
			.build();
	}
}
