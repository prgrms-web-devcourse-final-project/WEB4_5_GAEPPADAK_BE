package site.kkokkio.domain.batch.flow;

import static site.kkokkio.domain.batch.context.BatchConstants.*;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SearchSourcesFlowConfig {

	private final Step searchNewsStep;
	private final Step searchVideosStep;

	@Bean(name = SEARCH_SOURCES_FLOW)
	public Flow searchSourcesFlow() {
		return new FlowBuilder<SimpleFlow>(SEARCH_SOURCES_FLOW)
			.split(new SimpleAsyncTaskExecutor())
			.add(
				new FlowBuilder<Flow>(SEARCH_NEWS_FLOW)
					.start(searchNewsStep)
					.build(),

				new FlowBuilder<Flow>(SEARCH_VIDEO_FLOW)
					.start(searchVideosStep)
					.build()
			)
			.build();
	}
}