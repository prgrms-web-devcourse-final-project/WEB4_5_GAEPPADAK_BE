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
import io.micrometer.core.instrument.DistributionSummary;
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
			io.micrometer.core.instrument.Tag.of("step", "myStep"),
			io.micrometer.core.instrument.Tag.of("bucket", "bucket1")
		);
		DistributionSummary newsSum = registry.find(BATCH_NEWS_FETCHED)
			.tags(baseTags).summary();
		assertThat(newsSum.count()).isOne();
		assertThat(newsSum.totalAmount()).isEqualTo(5.0);

		DistributionSummary videoSum = registry.find(BATCH_VIDEO_FETCHED)
			.tags(baseTags).summary();
		assertThat(videoSum.count()).isOne();
		assertThat(videoSum.totalAmount()).isEqualTo(3.0);

		DistributionSummary lowVarSum = registry.find(BATCH_NOVELTY_LOWVAR)
			.tags(baseTags).summary();
		assertThat(lowVarSum.count()).isOne();
		assertThat(lowVarSum.totalAmount()).isEqualTo(2.0);

		DistributionSummary postSum = registry.find(BATCH_POST_CREATED)
			.tags(baseTags).summary();
		assertThat(postSum.count()).isOne();
		assertThat(postSum.totalAmount()).isEqualTo(4.0);

		// Counter metrics
		Counter newsFail = registry.find(BATCH_NEWS_API_FAIL_TOTAL)
			.tags(baseTags).counter();
		assertThat(newsFail.count()).isEqualTo(1.0);

		Counter videoFail = registry.find(BATCH_VIDEO_API_FAIL_TOTAL)
			.tags(baseTags).counter();
		assertThat(videoFail.count()).isEqualTo(0.0);

		// Gauge metrics
		Gauge cacheGauge = registry.find(BATCH_CACHE_SIZE)
			.tags(baseTags).gauge();
		assertThat(cacheGauge.value()).isEqualTo(7.0);
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