package site.kkokkio.global.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.MethodInvokingTaskletAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.service.GoogleTrendsRssService;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

	private final GoogleTrendsRssService googleTrendsRssService;
	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;

	// googleTrendsRssService의 getTrendingKeywordsFromRss 메소드를 Tasklet으로 매핑
	@Bean
	public MethodInvokingTaskletAdapter trendingKeywordsTasklet() {
		MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();
		adapter.setTargetObject(googleTrendsRssService);
		adapter.setTargetMethod("getTrendingKeywordsFromRss");
		return adapter;
	}

	// 매핑된 Tasklet을 실제 작업을 수행하는 Step으로 빌드
	@Bean
	public Step trendingKeywordsStep() {
		return new StepBuilder("trendingKeywordsStep", jobRepository)
			.tasklet(trendingKeywordsTasklet(), transactionManager)
			.build();
	}

	// 배치의 작업 단위인 Job을 정의하고 작업의 첫 단계(Step)를 만들어둔 trendingKeywordsStep로 작업하도록 설정
	// 해당 Job은 Scheduler를 통해 1시간 간격으로 실행되도록 설정
	// 만약 Job에 실행할 추가하고 싶다면 위와 동일하게 step 생성후 .next()와 같은 방식으로 사용
	@Bean
	public Job trendingKeywordsJob() {
		return new JobBuilder("trendingKeywordsJob", jobRepository)
			.incrementer(new RunIdIncrementer())
			.start(trendingKeywordsStep())
			.build();
	}
}