package site.kkokkio.domain.batch.context;

public interface MetricsKeys {
	String BATCH_NEWS_FETCHED = "spring_batch_news_fetched";
	String BATCH_NEWS_API_FAIL_TOTAL = "spring_batch_news_api_fail_total";
	String BATCH_VIDEO_FETCHED = "spring_batch_video_fetched";
	String BATCH_VIDEO_API_FAIL_TOTAL = "spring_batch_video_api_fail_total";
	String BATCH_NOVELTY_LOWVAR = "spring_batch_novelty_lowvar";
	String BATCH_POST_CREATED = "spring_batch_post_added";
	String BATCH_CACHE_SIZE = "spring_batch_post_cached";
	String BATCH_NO_POST_NEEDED = "spring_batch_no_post_needed";
}
