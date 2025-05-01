package site.kkokkio.domain.post.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyDto;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostKeyword;
import site.kkokkio.domain.post.entity.PostMetricHourly;
import site.kkokkio.domain.post.repository.PostKeywordRepository;
import site.kkokkio.domain.post.repository.PostMetricHourlyRepository;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.repository.KeywordSourceRepository;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
	private final PostRepository postRepository;
	private final KeywordRepository keywordRepository;
	private final KeywordMetricHourlyRepository keywordMetricHourlyRepository;
	private final PostKeywordRepository postKeywordRepository;
	private final PostMetricHourlyRepository postMetricHourlyRepository;
	private final KeywordSourceRepository keywordSourceRepository;
	private final KeywordMetricHourlyService keywordMetricHourlyService;
	private final PostSourceRepository postSourceRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public Post getPostById(Long id) {
		return postRepository.findById(id)
			.orElseThrow(() -> new ServiceException("404", "해당 포스트를 찾을 수 없습니다."));
	}

	@Transactional(readOnly = true)
	public PostDto getPostWithKeywordById(Long id) {
		Post post = postRepository.findById(id)
			.orElseThrow(() -> new ServiceException("404", "포스트를 불러오지 못했습니다."));

		PostKeyword postKeyword = postKeywordRepository.findByPost_Id(id)
			.orElseThrow(() -> new ServiceException("404", "포스트를 불러오지 못했습니다."));

		String keywordText = postKeyword.getKeyword().getText();

		return PostDto.from(post, keywordText);
	}

	@Transactional(readOnly = true)
	public List<PostDto> getTopPostsWithKeyword() {

		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
			.withMinute(0)
			.withSecond(0)
			.withNano(0);

		//최신 버킷 기준으로 점수 높은 순 키워드(10개)
		List<KeywordMetricHourly> topKeywordMetrics = keywordMetricHourlyRepository.findTop10ById_BucketAtOrderByScoreDesc(
			now);

		List<PostDto> topPosts = topKeywordMetrics.stream()
			.filter(metric -> metric.getPost() != null)
			.map(metric -> PostDto.from(metric.getPost(), metric.getKeyword().getText()))
			.toList();

		if (topPosts.isEmpty()) {
			throw new ServiceException("404", "포스트를 불러오지 못했습니다.");
		}

		return topPosts;
	}

	/**
	 * 신규 키워드에 대해서 포스트를 생성한다.
	 */
	@Transactional
	public void generatePosts() {
		// Step 1. 최신 시간대 기준 Top10 키워드 전체 조회
		List<KeywordMetricHourlyDto> allTopKeywords = keywordMetricHourlyService.findHourlyMetrics();

		for (KeywordMetricHourlyDto metric : allTopKeywords) {
			Long keywordId = metric.keywordId();
			String keywordText = metric.text();
			LocalDateTime bucketAt = metric.bucketAt();

			// Step 2. 해당 키워드 기준으로 수집된 Source 조회
			// TODO: executionContext로 수집한 new Source Url 리스트로 대체
			PageRequest pageRequest = PageRequest.of(0, 10);
			List<Source> sources = keywordSourceRepository.findSourcesByKeywordId(keywordId, pageRequest).getContent();

			if (sources.isEmpty()) {
				log.warn("Source 없음 → keyword={} 스킵", keywordText);
				continue;
			}

			// Step 3A. low_variation=true → 포스트 생성 스킵 + 기존 포스트 연결 처리
			if (metric.lowVariation()) {
				Post existingPost = getMostRecentPostByKeyword(keywordId);
				if (existingPost == null) {
					log.warn("기존 포스트 없음 → keyword={} 스킵", keywordText);
					continue;
				}

				// KeywordMetricHourly ↔ Post 연결
				KeywordMetricHourlyId keywordMetricHourlyId = new KeywordMetricHourlyId(bucketAt, Platform.GOOGLE_TREND,
					keywordId);
				KeywordMetricHourly keywordMetricHourly = keywordMetricHourlyRepository.findById(keywordMetricHourlyId)
					.orElseThrow(() -> new ServiceException("404", "KeywordMetricHourly를 찾을 수 없습니다."));

				keywordMetricHourly.setPost(existingPost);

				// Source ↔ Post 매핑
				linkSourcesToPost(existingPost, sources);

				continue;
			}

			// Step 3B. low_variation=false → 신규 Post 생성
			// TODO: AI 연결 전이므로 임시로 특정 Source의 Title, Description으로 작성
			String title = sources.getFirst().getTitle();
			String summary = sources.getFirst().getDescription();
			String thumbnailUrl = sources.getFirst().getThumbnailUrl();

			Post post = postRepository.save(
				Post.builder()
					.title(title)
					.summary(summary)
					.thumbnailUrl(thumbnailUrl)
					.bucketAt(bucketAt)
					.reportCount(0)
					.build()
			);

			// Source ↔ Post 매핑
			linkSourcesToPost(post, sources);

			// KeywordMetricHourly ↔ Post 연결
			KeywordMetricHourlyId keywordMetricHourlyId = new KeywordMetricHourlyId(bucketAt, Platform.GOOGLE_TREND,
				keywordId);
			KeywordMetricHourly keywordMetricHourly = keywordMetricHourlyRepository.findById(keywordMetricHourlyId)
				.orElseThrow(() -> new ServiceException("404", "KeywordMetricHourly를 찾을 수 없습니다."));

			keywordMetricHourly.setPost(post);

			// Keyword ↔ Post 매핑
			Keyword keyword = keywordRepository.findById(keywordId)
				.orElseThrow(() -> new ServiceException("404", "Keyword를 찾을 수 없습니다."));
			PostKeyword postKeyword = PostKeyword.builder()
				.post(post)
				.keyword(keyword)
				.build();
			postKeywordRepository.save(postKeyword);

			// PostMetricHourly 생성
			PostMetricHourly postMetricHourly = PostMetricHourly.builder()
				.post(post)
				.bucketAt(bucketAt)
				.clickCount(0)
				.likeCount(0)
				.build();
			postMetricHourlyRepository.save(postMetricHourly);

			// Redis 캐싱 (TTL 24시간)
			cachePostCardView(post, keywordText, Duration.ofHours(24));

			log.info("신규 포스트 생성 완료 - postId={}, keyword={}", post.getId(), keywordText);
		}
	}

	// 가장 최근 생성된 포스트 반환
	public Post getMostRecentPostByKeyword(Long keywordId) {
		return postKeywordRepository.findTopByKeywordIdOrderByPost_BucketAtDesc(keywordId)
			.map(PostKeyword::getPost)
			.orElse(null);
	}

	// Source 리스트 ↔ Post 매핑 저장
	public void linkSourcesToPost(Post post, List<Source> sources) {
		List<PostSource> mappings = sources.stream()
			.map(source -> PostSource.builder()
				.post(post)
				.source(source)
				.build())
			.toList();
		postSourceRepository.saveAll(mappings);
	}

	public void cachePostCardView(Post post, String keyword, Duration ttl) {
		ValueOperations<String, String> values = redisTemplate.opsForValue();
		String key = "POST_CARD:" + post.getId();

		PostDto dto = PostDto.from(post, keyword);
		try {
			String json = objectMapper.writeValueAsString(dto);
			values.set(key, json, ttl);
		} catch (JsonProcessingException e) {
			log.error("Redis 캐싱 직렬화 실패. postId={}", post.getId(), e);
		}
	}
}
