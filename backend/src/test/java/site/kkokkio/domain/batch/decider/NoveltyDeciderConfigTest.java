package site.kkokkio.domain.batch.decider;

import static org.assertj.core.api.Assertions.*;
import static site.kkokkio.domain.batch.context.BatchConstants.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.test.context.ActiveProfiles;

import site.kkokkio.domain.batch.context.ExecutionContextKeys;

@ActiveProfiles("test")
class NoveltyDeciderConfigTest {

	@InjectMocks
	private JobExecutionDecider decider;

	@BeforeEach
	void setUp() {
		decider = new NoveltyDeciderConfig().noveltyDecider();
	}

	@Test
	@DisplayName("noveltyDecider: 포스트할 키워드 없음")
	void testNoPostNeeded() {
		// given
		JobInstance instance = new JobInstance(1L, "testJob");
		JobExecution jobExecution = new JobExecution(instance, new JobParameters());
		jobExecution.getExecutionContext()
			.put(ExecutionContextKeys.JC_POSTABLE_KEYWORD_IDS, List.<Long>of());

		// when
		FlowExecutionStatus status = decider.decide(jobExecution, null);

		// then
		assertThat(status.getName())
			.isEqualTo(NO_POST_NEEDED_STATUS);
		Object flag = jobExecution.getExecutionContext()
			.get(ExecutionContextKeys.JC_NO_POST_NEEDED);
		assertThat(flag).isInstanceOf(Boolean.class)
			.isEqualTo(Boolean.TRUE);
	}

	@Test
	@DisplayName("noveltyDecider: 포스트할 키워드 존재")
	void testPostNeeded() {
		// given
		JobInstance instance = new JobInstance(2L, "testJob");
		JobExecution jobExecution = new JobExecution(instance, new JobParameters());
		jobExecution.getExecutionContext()
			.put(ExecutionContextKeys.JC_POSTABLE_KEYWORD_IDS, List.of(100L, 200L));

		// when
		FlowExecutionStatus status = decider.decide(jobExecution, null);

		// then
		assertThat(status)
			.isEqualTo(FlowExecutionStatus.COMPLETED);
		Object flag = jobExecution.getExecutionContext()
			.get(ExecutionContextKeys.JC_NO_POST_NEEDED);
		assertThat(flag).isInstanceOf(Boolean.class)
			.isEqualTo(Boolean.FALSE);
	}
}