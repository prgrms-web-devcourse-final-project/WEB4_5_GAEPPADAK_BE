package site.kkokkio.domain.batch.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BatchMetricsListener implements StepExecutionListener, JobExecutionListener {

	private final MeterRegistry meter;

	@Value("${spring.application.name:trend-batch}")
	private String application;

	@Value("${spring.profiles.active:local}")
	private String profile;

	// Step 종료 :  ExecutionContext → Gauge / Counter
	@Override
	public ExitStatus afterStep(StepExecution stepExec) {

		JobExecution je = stepExec.getJobExecution();
		ExecutionContext ctx = stepExec.getExecutionContext();

		Tags base = Tags.of(
			"application", application,
			"instance", profile,
			"job", je.getJobInstance().getJobName(),
			"step", stepExec.getStepName(),
			"bucket", String.valueOf(je.getExecutionContext().get("bucketAt"))
		);

		// Gauge & Counter 기록
		recordGauge(ctx, "rssLatencyMs", "batch_rss_latency_ms", base);
		recordCounter(ctx, "newsFetched", "batch_news_fetched_total", base);
		recordCounter(ctx, "newsApiFail", "batch_news_api_fail_total", base);
		recordCounter(ctx, "videoFetched", "batch_video_fetched_total", base);
		recordCounter(ctx, "videoApiFail", "batch_video_api_fail_total", base);
		recordCounter(ctx, "noveltyLowVarCount", "batch_novelty_lowvar_total", base);
		recordCounter(ctx, "postCreated", "batch_post_created_total", base);
		recordGauge(ctx, "cacheEntryCount", "batch_cache_size", base);

		return stepExec.getExitStatus();
	}

	// Job 종료 : noPostNeeded 플래그 Gauge
	@Override
	public void afterJob(JobExecution jobExec) {
		boolean skip = Boolean.TRUE.equals(jobExec.getExecutionContext().get("noPostNeeded"));

		Gauge.builder("batch_no_post_needed", () -> skip ? 1.0 : 0.0)
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
}