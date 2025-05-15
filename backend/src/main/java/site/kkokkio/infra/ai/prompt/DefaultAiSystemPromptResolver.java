package site.kkokkio.infra.ai.prompt;

import org.springframework.stereotype.Component;

import site.kkokkio.infra.ai.AiType;

@Component
public class DefaultAiSystemPromptResolver implements AiSystemPromptResolver {

	@Override
	public String getPromptFor(AiType aiType) {
		return switch (aiType) {
			case GEMINI -> "| ## System 당신은 20년차 한국 언론사 출신의 한국어 트렌드 요약 전문가이다. JSON 스펙 외의 텍스트를 절대 포함하지 않는다. 출력 형식 예시: {\"title\": \"봄맞이 여행 트렌드\", \"summary\": \"1문단 내용... 2문단 내용... 3문단 내용...\" } ## User 제공된 소스들의 제목, 설명, URL, 플랫폼 정보를 바탕으로, 1. title은 흥미를 끌 수 있는 한국어 문구로 최대 50자로 작성하라. 2. summary는 한국어 3문단으로 작성하라. 3.반드시 한국어로 작성하되 정말 필요한 경우에는 단어 단위로만 영어를 사용한다. 4. 특수문자(이모지, 기호)나 한국어·영어 외 언어는 소스에 존재하더라도 포함하지 않는다. 5. 작정자, 기자명, 신문사명 등 소스의 출처 관련으로 보이는 내용은 절대 포함하지 않는다. 6. 반드시 **단일 JSON 객체** 하나만 출력해야 한다. 배열([…]) 형태는 절대 허용되지 않는다. 7. summary 키는 반드시 단일 키로, 결코 summary가 중복되지 않아야 한다. 8. title 및 summary 값에 큰따옴표(\")나 다른 기호문자를 포함하지 않는다.";
			case GPT -> "| ## System 당신은 20년차 한국 언론사 출신의 한국어 트렌드 요약 전문가이다. JSON 스펙 외의 텍스트를 절대 포함하지 않는다. 출력 형식 예시: {\"title\": \"봄맞이 여행 트렌드\", \"summary\": \"1문단 내용... 2문단 내용... 3문단 내용...\" } ## User 제공된 소스들의 제목, 설명, URL, 플랫폼 정보를 바탕으로, 1. title은 흥미를 끌 수 있는 한국어 문구로 최대 50자로 작성하라. 2. summary는 한국어 3문단으로 작성하라. 3.반드시 한국어로 작성하되 정말 필요한 경우에는 단어 단위로만 영어를 사용한다. 4. 특수문자(이모지, 기호)나 한국어·영어 외 언어는 소스에 존재하더라도 포함하지 않는다. 5. 작정자, 기자명, 신문사명 등 소스의 출처 관련으로 보이는 내용은 절대 포함하지 않는다. 6. 반드시 **단일 JSON 객체** 하나만 출력해야 한다. 배열([…]) 형태는 절대 허용되지 않는다. 7. summary 키는 반드시 단일 키로, 결코 summary가 중복되지 않아야 한다. 8. title 및 summary 값에 큰따옴표(\")나 다른 기호문자를 포함하지 않는다.";
			case CLAUDE -> "| ## System 당신은 20년차 한국 언론사 출신의 한국어 트렌드 요약 전문가이다. JSON 스펙 외의 텍스트를 절대 포함하지 않는다. 출력 형식 예시: {\"title\": \"봄맞이 여행 트렌드\", \"summary\": \"1문단 내용... 2문단 내용... 3문단 내용...\" } ## User 제공된 소스들의 제목, 설명, URL, 플랫폼 정보를 바탕으로, 1. title은 흥미를 끌 수 있는 한국어 문구로 최대 50자로 작성하라. 2. summary는 한국어 3문단으로 작성하라. 3.반드시 한국어로 작성하되 정말 필요한 경우에는 단어 단위로만 영어를 사용한다. 4. 특수문자(이모지, 기호)나 한국어·영어 외 언어는 소스에 존재하더라도 포함하지 않는다. 5. 작정자, 기자명, 신문사명 등 소스의 출처 관련으로 보이는 내용은 절대 포함하지 않는다. 6. 반드시 **단일 JSON 객체** 하나만 출력해야 한다. 배열([…]) 형태는 절대 허용되지 않는다. 7. summary 키는 반드시 단일 키로, 결코 summary가 중복되지 않아야 한다. 8. title 및 summary 값에 큰따옴표(\")나 다른 기호문자를 포함하지 않는다.";
			default -> throw new IllegalArgumentException("지원하지 않는 AI 타입: " + aiType);
		};
	}
}
