package site.kkokkio.global.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.MethodInvokingTaskletAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.service.GoogleTrendsRssService;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.service.SourceService;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;

	private final GoogleTrendsRssService googleTrendsRssService;
	private final SourceService sourceService;
	private final KeywordMetricHourlyService keywordMetricHourlyService;
	private final PostService postService;

	/**
	 * Step 1: Google 트렌드 키워드 수집
	 */
	@Bean
	public MethodInvokingTaskletAdapter fetchTrendingKeywordsTasklet() {
		MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();
		adapter.setTargetObject(googleTrendsRssService);
		adapter.setTargetMethod("getTrendingKeywordsFromRss"); // 내부에서 Top10(KeywordMetricHourly)까지 DB 저장
		return adapter;
	}

	@Bean
	public Step fetchTrendingKeywordsStep() {
		return new StepBuilder("fetchTrendingKeywordsStep", jobRepository)
			.tasklet(fetchTrendingKeywordsTasklet(), transactionManager)
			.build();
	}

	/**
	 * Step 2-1: 뉴스 검색
	 */
	@Bean
	public MethodInvokingTaskletAdapter searchNewsTasklet() {
		MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();
		adapter.setTargetObject(sourceService);
		adapter.setTargetMethod("searchNews");
		return adapter;
	}

	@Bean
	public Step searchNewsStep() {
		return new StepBuilder("searchNewsStep", jobRepository)
			.tasklet(searchNewsTasklet(), transactionManager)
			.build();
	}

	/**
	 * Step 2-2: 유튜브 검색
	 */
	@Bean
	public MethodInvokingTaskletAdapter searchVideosTasklet() {
		MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();
		adapter.setTargetObject(sourceService);
		adapter.setTargetMethod("searchYoutube");
		return adapter;
	}

	@Bean
	public Step searchVideosStep() {
		return new StepBuilder("searchVideosStep", jobRepository)
			.tasklet(searchVideosTasklet(), transactionManager)
			.build();
	}

	/**
	 * Step 2: 뉴스/유튜브 병렬 검색 Flow
	 */
	@Bean
	public Flow searchSourcesFlow() {
		return new FlowBuilder<SimpleFlow>("searchSourcesFlow")
			.split(new SimpleAsyncTaskExecutor())
			.add(
				new FlowBuilder<Flow>("searchNewsFlow").start(searchNewsStep()).build(),
				new FlowBuilder<Flow>("searchVideosFlow").start(searchVideosStep()).build()
			)
			.build();
	}

	@Bean
	public Step searchSourcesStep() {
		return new StepBuilder("searchSourcesStep", jobRepository)
			.flow(searchSourcesFlow())
			.build();
	}

	/**
	 * Step 3: 신규성 평가
	 */
	@Bean
	public MethodInvokingTaskletAdapter evaluateNoveltyTasklet() {
		MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();
		adapter.setTargetObject(keywordMetricHourlyService);
		adapter.setTargetMethod("evaluateNovelty");
		return adapter;
	}

	@Bean
	public Step evaluateNoveltyStep() {
		return new StepBuilder("evaluateNoveltyStep", jobRepository)
			.tasklet(evaluateNoveltyTasklet(), transactionManager)
			.build();
	}

	/**
	 * Step 4: 포스트 생성
	 */
	@Bean
	public MethodInvokingTaskletAdapter generatePostTasklet() {
		MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();
		adapter.setTargetObject(postService);
		adapter.setTargetMethod("generatePosts");
		return adapter;
	}

	@Bean
	public Step generatePostStep() {
		return new StepBuilder("generatePostStep", jobRepository)
			.tasklet(generatePostTasklet(), transactionManager)
			.build();
	}
	/**
	 * 전체 Job 정의
	 * Scheduler를 통해 1시간 간격으로 실행되도록 설정됨.
	 */
	@Bean
	public Job trendToPostJob() {
		return new JobBuilder("trendToPostJob", jobRepository)
			.incrementer(new RunIdIncrementer())
			.start(fetchTrendingKeywordsStep())               // Step 1
			.next(searchSourcesStep())                        // Step 2
			.next(evaluateNoveltyStep())                      // Step 3
			.next(generatePostStep())                         // Step 4
			.build();
	}

}