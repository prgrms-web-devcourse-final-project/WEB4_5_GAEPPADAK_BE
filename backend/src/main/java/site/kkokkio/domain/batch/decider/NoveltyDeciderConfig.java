package site.kkokkio.domain.batch.decider;

import static site.kkokkio.domain.batch.context.BatchConstants.*;
import static site.kkokkio.domain.batch.context.ExecutionContextKeys.*;

import java.util.List;

import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class NoveltyDeciderConfig {

	@Bean(name = NOVELTY_DECIDER)
	public JobExecutionDecider noveltyDecider() {
		return (jobExec, stepExec) -> {

			// low-variation 키워드 유무 판정
			@SuppressWarnings("unchecked")
			List<Long> ids = (List<Long>)jobExec.getExecutionContext()
				.get(JC_POSTABLE_KEYWORD_IDS);

			boolean noPostNeeded = ids.isEmpty();

			jobExec.getExecutionContext().put(JC_NO_POST_NEEDED, noPostNeeded);

			return noPostNeeded
				? new FlowExecutionStatus(NO_POST_NEEDED_STATUS)
				: FlowExecutionStatus.COMPLETED;
		};
	}
}