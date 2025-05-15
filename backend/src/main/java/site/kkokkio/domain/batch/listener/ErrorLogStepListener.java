package site.kkokkio.domain.batch.listener;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ErrorLogStepListener implements StepExecutionListener,
	SkipListener<Object, Object>,
	ChunkListener {

	@Override
	public void beforeStep(StepExecution stepExecution) {
		log.info("[BEFORE STEP] [{}] started (id={})",
			stepExecution.getStepName(),
			stepExecution.getId());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		log.info("[AFTER STEP]  [{}] finished (read={}, write={}, skip={}, commit={}, rollback={})",
			stepExecution.getStepName(),
			stepExecution.getReadCount(),
			stepExecution.getWriteCount(),
			stepExecution.getSkipCount(),
			stepExecution.getCommitCount(),
			stepExecution.getRollbackCount());
		return stepExecution.getExitStatus();
	}

	@Override
	public void onSkipInRead(Throwable t) {
		log.error("[READ-SKIP] {}", t.getMessage(), t);
	}

	@Override
	public void onSkipInProcess(Object item, Throwable t) {
		log.error("[PROCESS-SKIP] item={}, err={}", safe(item), t.toString());
	}

	@Override
	public void onSkipInWrite(Object item, Throwable t) {
		log.error("[WRITE-SKIP] item={}, err={}", safe(item), t.toString());
	}

	@Override
	public void afterChunkError(ChunkContext context) {
		Object raw = context.getAttribute(ChunkListener.ROLLBACK_EXCEPTION_KEY);
		if (raw instanceof Throwable ex) {
			log.warn("[CHUNK-ROLLBACK] step={}, msg={}",
				context.getStepContext().getStepName(),
				ex.getMessage(), ex);
		}
	}

	private String safe(Object item) {
		try {
			return String.valueOf(item);
		} catch (Exception e) {
			return "Unprintable Item";
		}
	}
}