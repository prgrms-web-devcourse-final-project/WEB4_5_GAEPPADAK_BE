package site.kkokkio.domain.batch.context;

public interface ExecutionContextKeys {

	// JobExecutionContext Keys
	String EC_BUCKET_AT = "bucketAt";
	String EC_TOP_IDS = "topIds";
	String EC_TOP_COUNT = "topCount";
	String EC_POSTABLE_IDS = "postableIds";
	String EC_POSTABLE_COUNT = "postableCount";
	String EC_NO_POST_NEEDED = "noPostNeeded";

	// StepExecutionContext Keys
	String SC_RSS_LATENCY_MS = "rssLatencyMs";
	String SC_NEWS_FETCHED = "newsFetched";
	String SC_NEWS_API_FAIL = "newsApiFail";
	String SC_VIDEO_FETCHED = "videoFetched";
	String SC_VIDEO_API_FAIL = "videoApiFail";
	String SC_NOVELTY_SKIPPED = "noveltyLowVarCount";
	String SC_POST_CREATED = "postCreated";
	String SC_CACHE_SIZE = "cacheEntryCount";
}