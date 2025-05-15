package site.kkokkio.domain.batch.context;

public interface BatchConstants {
	// Batch Job
	String TREND_TO_POST_JOB = "trendToPostJob";

	// Batch Steps
	String FETCH_KEYWORDS_STEP = "fetchTrendingKeywordsStep";
	String SEARCH_NEWS_STEP = "searchNewsStep";
	String SEARCH_VIDEOS_STEP = "searchVideosStep";
	String EVALUATE_NOVELTY_STEP = "evaluateNoveltyStep";
	String GENERATE_POST_STEP = "generatePostStep";
	String CACHE_POST_STEP = "cachePostStep";

	// Batch Flows
	String SEARCH_SOURCES_FLOW = "searchSourcesFlow";
	String SEARCH_NEWS_FLOW = "searchNewsFlow";
	String SEARCH_VIDEO_FLOW = "searchVideosFlow";
}
