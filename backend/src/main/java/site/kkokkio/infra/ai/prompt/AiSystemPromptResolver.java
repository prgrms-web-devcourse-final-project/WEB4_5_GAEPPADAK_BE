package site.kkokkio.infra.ai.prompt;

import site.kkokkio.infra.ai.AiType;

public interface AiSystemPromptResolver {
	String getPromptFor(AiType aiType);
}
