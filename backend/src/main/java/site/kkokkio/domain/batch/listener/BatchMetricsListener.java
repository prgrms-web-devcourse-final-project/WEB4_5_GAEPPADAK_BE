package site.kkokkio.domain.batch.listener;

import static site.kkokkio.domain.batch.context.ExecutionContextKeys.*;
import static site.kkokkio.domain.batch.context.JobParameterKeys.*;
import static site.kkokkio.domain.batch.context.MetricsKeys.*;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BatchMetricsListener implements StepExecutionListener, JobExecutionListener {

	private final MeterRegistry meter;

	@Value("${management.metrics.tags.application:trend-batch}")
	private String application;

	@Value("${spring.profiles.active:local}")
	private String profile;

	// Step 종료 :  ExecutionContext → Gauge / Counter
	@Override
	public ExitStatus afterStep(StepExecution stepExec) {

		JobExecution je = stepExec.getJobExecution();
		ExecutionContext ctx = stepExec.getExecutionContext();
		JobParameters jp = stepExec.getJobExecution().getJobParameters();

		Tags base = Tags.of(
			"application", application,
			"instance", profile,
			"job", je.getJobInstance().getJobName(),
			"step", stepExec.getStepName(),
			"bucket", jp.getString(JP_RUNTIME)
		);

		// Summary & Gauge & Counter 기록
		recordSummary(ctx, SC_NEWS_FETCHED, BATCH_NEWS_FETCHED, base);
		recordSummary(ctx, SC_VIDEO_FETCHED, BATCH_VIDEO_FETCHED, base);
		recordSummary(ctx, SC_NOVELTY_SKIPPED, BATCH_NOVELTY_LOWVAR, base);
		recordSummary(ctx, SC_POST_CREATED, BATCH_POST_CREATED, base);

		recordCounter(ctx, SC_NEWS_API_FAIL, BATCH_NEWS_API_FAIL_TOTAL, base);
		recordCounter(ctx, SC_VIDEO_API_FAIL, BATCH_VIDEO_API_FAIL_TOTAL, base);

		recordGauge(ctx, SC_CACHE_SIZE, BATCH_CACHE_SIZE, base);

		return stepExec.getExitStatus();
	}

	// Job 종료 : noPostNeeded 플래그 Gauge
	@Override
	public void afterJob(JobExecution jobExec) {
		boolean skip = Boolean.TRUE.equals(jobExec.getExecutionContext().get("noPostNeeded"));

		Gauge.builder(BATCH_NO_POST_NEEDED, () -> skip ? 1.0 : 0.0)
			.tags("application", application,
				"instance", System.getenv().getOrDefault("HOSTNAME", "local"),
				"job", jobExec.getJobInstance().getJobName())
			.register(meter);
	}

	private void recordGauge(ExecutionContext ctx, String key, String name, Tags tags) {
		if (ctx.containsKey(key)) {
			Number number = ctx.get(key, Number.class);
			Gauge.builder(name, number::doubleValue)
				.tags(tags)
				.register(meter);
		}
	}

	private void recordCounter(ExecutionContext ctx, String key, String name, Tags tags) {
		if (ctx.containsKey(key)) {
			Number number = ctx.get(key, Number.class);
			Counter counter = Counter.builder(name)
				.tags(tags)
				.register(meter);
			counter.increment(number.doubleValue());
		}
	}

	private void recordSummary(ExecutionContext ctx, String key, String name, Tags tags) {
		if (ctx.containsKey(key)) {
			double value = ctx.get(key, Number.class).doubleValue();
			DistributionSummary.builder(name)
				.tags(tags)
				.publishPercentileHistogram()
				.publishPercentiles(0.5, 0.9, 0.95, 0.99)
				.register(meter)
				.record(value);
		}
	}
}