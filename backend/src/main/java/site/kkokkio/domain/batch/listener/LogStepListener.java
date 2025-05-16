package site.kkokkio.domain.batch.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LogStepListener implements StepExecutionListener {

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
}