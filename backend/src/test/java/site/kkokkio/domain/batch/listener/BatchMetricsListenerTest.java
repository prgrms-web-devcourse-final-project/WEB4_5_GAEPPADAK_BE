package site.kkokkio.domain.batch.listener;

import static org.assertj.core.api.Assertions.*;
import static site.kkokkio.domain.batch.context.ExecutionContextKeys.*;
import static site.kkokkio.domain.batch.context.JobParameterKeys.*;
import static site.kkokkio.domain.batch.context.MetricsKeys.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.test.util.ReflectionTestUtils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class BatchMetricsListenerTest {

	private SimpleMeterRegistry registry;
	private BatchMetricsListener listener;

	@BeforeEach
	void setUp() {
		registry = new SimpleMeterRegistry();
		listener = new BatchMetricsListener(registry);
		ReflectionTestUtils.setField(listener, "application", "trend-batch-test");
		ReflectionTestUtils.setField(listener, "profile", "test");
	}

	@Test
	@DisplayName("BatchMetricsListener afterStep 성공")
	void testAfterStepMetricsRecorded() {
		// given
		var jp = new JobParametersBuilder()
			.addString(JP_RUNTIME, "bucket1")
			.toJobParameters();
		var je = new JobExecution(new JobInstance(1L, "myJob"), jp);

		// when
		StepExecution se = new StepExecution("myStep", je);
		se.getExecutionContext().put(SC_NEWS_FETCHED, 5);
		se.getExecutionContext().put(SC_VIDEO_FETCHED, 3);
		se.getExecutionContext().put(SC_NOVELTY_SKIPPED, 2);
		se.getExecutionContext().put(SC_POST_CREATED, 4);
		se.getExecutionContext().put(SC_NEWS_API_FAIL, 1);
		se.getExecutionContext().put(SC_VIDEO_API_FAIL, 0);
		se.getExecutionContext().put(SC_CACHE_SIZE, 7);

		// when
		listener.afterStep(se);

		// then
		Set<io.micrometer.core.instrument.Tag> baseTags = Set.of(
			io.micrometer.core.instrument.Tag.of("application", "trend-batch-test"),
			io.micrometer.core.instrument.Tag.of("instance", "test"),
			io.micrometer.core.instrument.Tag.of("job", "myJob"),
			io.micrometer.core.instrument.Tag.of("step", "myStep")
		);
		// Counter metrics
		Counter newsSum = registry.find(BATCH_NEWS_FETCHED)
			.tags(baseTags).counter();
		assertThat(newsSum.count()).isEqualTo(5.0);

		Counter videoSum = registry.find(BATCH_VIDEO_FETCHED)
			.tags(baseTags).counter();
		assertThat(videoSum.count()).isEqualTo(3.0);

		Counter lowVarSum = registry.find(BATCH_NOVELTY_LOWVAR)
			.tags(baseTags).counter();
		assertThat(lowVarSum.count()).isEqualTo(2.0);

		Counter postSum = registry.find(BATCH_POST_CREATED)
			.tags(baseTags).counter();
		assertThat(postSum.count()).isEqualTo(4.0);

		Counter newsFail = registry.find(BATCH_NEWS_API_FAIL_TOTAL)
			.tags(baseTags).counter();
		assertThat(newsFail.count()).isEqualTo(1.0);

		Counter videoFail = registry.find(BATCH_VIDEO_API_FAIL_TOTAL)
			.tags(baseTags).counter();
		assertThat(videoFail.count()).isEqualTo(0.0);

		Counter cacheGauge = registry.find(BATCH_CACHE_SIZE)
			.tags(baseTags).counter();
		assertThat(cacheGauge.count()).isEqualTo(7.0);
	}

	@Test
	@DisplayName("BatchMetricsListener afterStep: Key 없음")
	void testAfterStepMissingKeys() {
		// given
		var jp = new JobParametersBuilder()
			.addString(JP_RUNTIME, "bucketX")
			.toJobParameters();
		JobExecution je = new JobExecution(new JobInstance(99L, "jobX"), jp);

		StepExecution se = new StepExecution("stepX", je);
		// when
		listener.afterStep(se);

		// then
		assertThat(registry.getMeters()).isEmpty();
	}

	@Test
	@DisplayName("BatchMetricsListener afterStep: noPostNeeded=True")
	void testAfterJobGauge() {
		// given
		JobExecution je = new JobExecution(new JobInstance(5L, "jobY"),
			new JobParametersBuilder().toJobParameters());
		je.getExecutionContext().put("noPostNeeded", Boolean.TRUE);

		// when
		listener.afterJob(je);

		// given
		Gauge g = registry.find(BATCH_NO_POST_NEEDED)
			.tags("application", "trend-batch-test",
				"instance", System.getenv().getOrDefault("HOSTNAME", "local"),
				"job", "jobY")
			.gauge();
		assertThat(g.value()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("BatchMetricsListener afterStep: noPostNeeded=false")
	void testAfterJobGaugeFalse() {
		// given
		JobExecution je = new JobExecution(new JobInstance(6L, "jobZ"),
			new JobParametersBuilder().toJobParameters());
		je.getExecutionContext().put("noPostNeeded", Boolean.FALSE);

		// when
		listener.afterJob(je);

		// given
		Gauge g = registry.find(BATCH_NO_POST_NEEDED)
			.tags("application", "trend-batch-test",
				"instance", System.getenv().getOrDefault("HOSTNAME", "local"),
				"job", "jobZ")
			.gauge();
		assertThat(g.value()).isEqualTo(0.0);
	}
}