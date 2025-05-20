package site.kkokkio.domain.batch.context;

public interface ExecutionContextKeys {

	// JobExecutionContext Keys
	String JC_TOP_KEYWORD_IDS = "topKeywordIds";
	String JC_TOP_KEYWORD_COUNT = "topKeywordCount";
	String JC_POSTABLE_KEYWORD_IDS = "postableKeywordIds";
	String JC_POSTABLE_KEYWORD_COUNT = "postableKeywordCount";
	String JC_NO_POST_NEEDED = "noPostNeeded";
	String JC_NEW_POST_IDS = "newPostIds";

	// StepExecutionContext Keys
	String SC_NEWS_FETCHED = "newsFetched";
	String SC_NEWS_API_FAIL = "newsApiFail";
	String SC_VIDEO_FETCHED = "videoFetched";
	String SC_VIDEO_API_FAIL = "videoApiFail";
	String SC_NOVELTY_SKIPPED = "noveltyLowVarCount";
	String SC_POST_CREATED = "postCreated";
	String SC_CACHE_SIZE = "cacheEntryCount";
}