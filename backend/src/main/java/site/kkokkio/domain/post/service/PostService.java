package site.kkokkio.domain.post.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyDto;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.domain.post.controller.dto.PostReportRequest;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.dto.ReportedPostSummary;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostKeyword;
import site.kkokkio.domain.post.entity.PostMetricHourly;
import site.kkokkio.domain.post.entity.PostReport;
import site.kkokkio.domain.post.port.out.AiSummaryPort;
import site.kkokkio.domain.post.repository.PostKeywordRepository;
import site.kkokkio.domain.post.repository.PostMetricHourlyRepository;
import site.kkokkio.domain.post.repository.PostReportRepository;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.domain.source.entity.KeywordSource;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.repository.KeywordSourceRepository;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.enums.ReportProcessingStatus;
import site.kkokkio.global.enums.ReportReason;
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
	private final PostReportRepository postReportRepository;
	private final KeywordMetricHourlyService keywordMetricHourlyService;
	private final PostSourceRepository postSourceRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final AiSummaryPort aiSummaryPort;
	private final MemberService memberService;

	public Post getPostById(Long id) {
		return postRepository.findById(id)
			.orElseThrow(() -> new ServiceException("404", "해당 포스트를 찾을 수 없습니다."));
	}

	@Transactional(readOnly = true)
	public PostDto getPostWithKeywordById(Long id, UserDetails userDetails) {
		Post post = postRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ServiceException("404", "포스트를 불러오지 못했습니다."));

		PostKeyword postKeyword = postKeywordRepository.findByPost_Id(id)
			.orElseThrow(() -> new ServiceException("404", "포스트를 불러오지 못했습니다."));

		String keywordText = postKeyword.getKeyword().getText();

		return PostDto.from(post, keywordText, isReportedByMe(userDetails, post));
	}

	private Boolean isReportedByMe(UserDetails userDetails, Post post) {
		if (userDetails == null) {
			return null;
		}
		Member member = memberService.findByEmail(userDetails.getUsername());

		return postReportRepository.existsByPostAndReporter(post, member);
	}

	@Transactional(readOnly = true)
	public List<PostDto> getTopPostsWithKeyword() {
		LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		String formattedNow = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
		List<KeywordMetricHourly> topKeywordMetrics =
			keywordMetricHourlyRepository.findTop10HourlyMetricsClosestToNowNative(formattedNow);

		return topKeywordMetrics.stream()
			.filter(metric -> {
				if (metric.getPost() == null) {
					log.error("keywordMetricHourlyId: {} 해당 키워드에 post가 존재하지 않습니다.", metric.getId());
					return false;
				} else if (metric.getPost().isDeleted()) {
					return false;
				}
				return true;
			}) // post_id가 null인 키워드 발견 시 서버 문제 예외처리
			.map(metric -> PostDto.from(metric.getPost(), metric.getKeyword().getText()))
			.toList();
	}

	/**
	 * 비동기로 요약을 요청한다.
	 */
	@Async
	public CompletableFuture<String> summarizeAsync(String content) {
		return aiSummaryPort.summarize(null, content);
	}

	/**
	 * 중복 제거를 위해 분리
	 */
	private Post savePost(String title, String summary, List<Source> sources, LocalDateTime bucketAt) {
		// sources 리스트를 순회하면서 첫 번째 non-null 썸네일 URL을 찾는다.
		String thumbnailUrl = sources.stream()
			.map(Source::getThumbnailUrl)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);

		Post post = Post.builder()
			.title(title)
			.summary(summary)
			.thumbnailUrl(thumbnailUrl)
			.bucketAt(bucketAt)
			.reportCount(0)
			.build();

		return postRepository.save(post);
	}

	/**
	 * 요약에서 오류 발생 시 Fallback 처리
	 */
	private Long createPostAndRelations(String title, String summary, List<Source> sources, LocalDateTime bucketAt,
		Long keywordId) {

		// 1) Post 엔티티 생성
		Post post = savePost(title, summary, sources, bucketAt);

		// 2) 신규 Post 연결
		// Source ↔ Post 매핑
		linkSourcesToPost(post, sources);

		// KeywordMetricHourly ↔ Post 연결
		KeywordMetricHourlyId keywordMetricHourlyId = new KeywordMetricHourlyId(bucketAt, Platform.GOOGLE_TREND,
			keywordId);
		KeywordMetricHourly keywordMetricHourly = keywordMetricHourlyRepository.findById(keywordMetricHourlyId)
			.orElseThrow(() -> new ServiceException("404", "KeywordMetricHourly를 찾을 수 없습니다."));
		keywordMetricHourly.setPost(post);

		// 3) Keyword ↔ Post 매핑
		Keyword keyword = keywordRepository.findById(keywordId)
			.orElseThrow(() -> new ServiceException("404", "Keyword를 찾을 수 없습니다."));
		PostKeyword postKeyword = PostKeyword.builder().post(post).keyword(keyword).build();
		postKeywordRepository.insertIgnoreAll(List.of(postKeyword));

		// 4) PostMetricHourly 생성
		PostMetricHourly postMetricHourly = PostMetricHourly.builder()
			.post(post)
			.bucketAt(bucketAt)
			.clickCount(0)
			.likeCount(0)
			.build();
		postMetricHourlyRepository.save(postMetricHourly);

		log.info("신규 포스트 생성 완료 - postId={}, keyword={}", post.getId(), keywordId);
		return post.getId();
	}

	/**
	 * 신규 키워드에 대해서 포스트를 생성한다.
	 */
	@Transactional
	public List<Long> generatePosts(List<Long> keywordIds) {
		// Step 1. 현재 Top10 키워드 전체 조회
		List<KeywordMetricHourlyDto> allTopKeywords = keywordMetricHourlyService.findHourlyMetrics();
		// List<Long> keywordIds = allTopKeywords.stream().map(KeywordMetricHourlyDto::keywordId).toList();

		// Step 2. Top10 keywords → sources 맵 조회
		// TODO: executionContext로 수집한 new Source Url 리스트로 대체
		List<KeywordSource> keywordSources = keywordSourceRepository.findTopSourcesByKeywordIdsLimited(keywordIds, 10);
		Map<Long, List<Source>> keywordToSources = KeywordSource.groupByKeywordId(keywordSources);

		List<Long> newPostIds = new ArrayList<>();

		for (KeywordMetricHourlyDto metric : allTopKeywords) {
			Long keywordId = metric.keywordId();
			// String keywordText = metric.text();
			LocalDateTime bucketAt = metric.bucketAt();

			List<Source> sources = keywordToSources.getOrDefault(keywordId, List.of());
			if (sources.isEmpty()) {
				log.warn("Source 없음 → keyword={} 스킵", keywordId);
				continue;
			}

			// Step 3A. low_variation=true → 포스트 생성 스킵 + 기존 포스트 연결
			if (metric.lowVariation()) {
				Post existingPost = getMostRecentPostByKeyword(keywordId);
				if (existingPost == null) {
					log.warn("기존 포스트 없음 → keyword={} 스킵", keywordId);
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

			// 1) 여러 소스 내용을 하나의 userContent로 합치기
			StringBuilder sb = new StringBuilder();
			for (Source src : sources) {
				sb.append("제목: ").append(src.getTitle()).append("\n")
					.append("설명: ").append(src.getDescription()).append("\n")
					.append("URL: ").append(src.getNormalizedUrl()).append("\n")
					.append("플랫폼: ").append(src.getPlatform()).append("\n\n");
			}
			String userContent = sb.toString();

			String aiResponse = null;
			String postTitle;
			String postSummary;

			try {
				aiResponse = summarizeAsync(userContent).get();
				log.info("[AI 응답(raw)] {}", aiResponse);
			} catch (Exception e) {
				log.error("AI 요약 실패, keyword={} → fallback 사용", keywordId, e);
				postTitle = sources.getFirst().getTitle();
				postSummary = sources.getFirst().getDescription();
				// 포스트 생성 및 관련 링크 생성
				Long postId = createPostAndRelations(postTitle, postSummary, sources, bucketAt, keywordId);
				newPostIds.add(postId);
				continue;
			}

			// 2) 마크다운 펜스 제거
			String jsonResponse = aiResponse
				.replaceAll("(?m)^```(?:json)?\\s*", "")
				.replaceAll("(?m)```\\s*$", "")
				.trim();

			// 3) JSON 파싱
			try {
				JsonNode root = objectMapper.readTree(jsonResponse);

				String rawTitle = root.path("title").asText(null);
				String rawSummary = root.path("summary").asText(null);

				if (rawTitle == null || rawSummary == null) {
					//키 누락 시 폴백 처리 - 첫 소스의 제목과 요약을 사용
					log.info("AI 응답 누락, 포스트 작성에 첫 번째 소스 사용");
					postTitle = sources.get(0).getTitle();
					postSummary = sources.get(0).getDescription();
				} else {
					postTitle = rawTitle;
					postSummary = rawSummary + "\n\nGenerated By AI";
				}

			} catch (IOException | JsonProcessingException e) {
				log.error("AI 응답 파싱 실패, keyword={} → fallback 사용", keywordId, e);
				postTitle = sources.get(0).getTitle();
				postSummary = sources.get(0).getDescription();
			}
			// 4) 포스트 생성 및 관련 링크 생성
			Long postId = createPostAndRelations(postTitle, postSummary, sources, bucketAt, keywordId);
			newPostIds.add(postId);
		}
		return newPostIds;
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
			.map(source -> PostSource.builder().post(post).source(source).build())
			.toList();
		postSourceRepository.insertIgnoreAll(mappings);
	}

	public int cacheCardViews(List<Long> postIds, List<Long> keywordIds, Duration ttl) {
		Set<Long> keywordIdSet = new HashSet<>(keywordIds); // 빠른 조회용

		List<PostKeyword> postKeywords = postKeywordRepository.findAllByPostIdIn(postIds);

		int cached = 0;

		for (PostKeyword pk : postKeywords) {
			Long keywordId = pk.getKeyword().getId();

			if (!keywordIdSet.contains(keywordId)) {
				continue;
			}

			boolean isCached = cachePostCardView(pk.getPost(), pk.getKeyword().getText(), ttl);
			if (isCached) {
				cached++;
			}
		}

		return cached;
	}

	public boolean cachePostCardView(Post post, String keyword, Duration ttl) {
		ValueOperations<String, String> values = redisTemplate.opsForValue();
		String key = "POST_CARD:" + post.getId();

		try {
			PostDto dto = PostDto.from(post, keyword);
			String json = objectMapper.writeValueAsString(dto);
			values.set(key, json, ttl);
			return true;
		} catch (JsonProcessingException e) {
			log.error("Redis 캐싱 직렬화 실패. postId={}", post.getId(), e);
			return false;
		}
	}

	/**
	 * 포스트 신고 기능
	 * @param postId 신고 대상 포스트 ID
	 * @param userDetails 신고하는 사용자
	 * @param request 신고 정보 DTO
	 */
	@Transactional
	public void reportPost(Long postId, UserDetails userDetails, PostReportRequest request) {

		// 1. 신고 대상 포스트 조회
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new ServiceException("404", "존재하지 않는 포스트입니다."));

		Member reporter = memberService.findByEmail(userDetails.getUsername());

		// 2. 삭제된 포스트인지 확인
		if (post.isDeleted()) {
			throw new ServiceException("400", "삭제된 포스트는 신고할 수 없습니다.");
		}

		// 3. 중복 신고 방지
		boolean alreadyReported = postReportRepository.existsByPostAndReporter(post, reporter);

		if (alreadyReported) {
			throw new ServiceException("400", "이미 신고한 포스트입니다.");
		}

		// 4. 기타 사유 선택 시 etcReason 필수 입력 검증
		if (request.reason() == ReportReason.ETC) {
			// etcReason이 null이거나, 공백만 있거나, 비어있으면 오류
			if (request.etcReason() == null || request.etcReason().trim().isEmpty()) {
				throw new ServiceException("400", "기타 사유 선택 시 상세 내용을 입력해야 합니다.");
			}
		}

		// 5. 신고 정보 생성
		PostReport report = PostReport.builder()
			.post(post)
			.reporter(reporter)
			.reason(request.reason())
			.etcReason(request.reason() == ReportReason.ETC ? request.etcReason().trim() : null)
			.build();

		// 6. 신고 정보 저장
		postReportRepository.save(report);

		// 7. 포스트의 신고 카운트 증가 및 저장
		post.incrementReportCount();
		postRepository.save(post);
	}

	/**
	 * 관리자용 신고된 포스트 목록을 페이징, 정렬, 검색하여 조회합니다.
	 * @param pageable 페이징 및 정렬 정보
	 * @param searchTarget 검색 대상 필드
	 * @param searchValue 검색어
	 * @return 페이징된 ReportedPostSummary 목록
	 */
	@Transactional(readOnly = true)
	public Page<ReportedPostSummary> getReportedPostsList(
		Pageable pageable, String searchTarget, String searchValue
	) {
		// 1. 정렬 옵션 검증 및 Repository 쿼리 별칭에 맞게 매핑
		Map<String, String> sortPropertyMapping = new HashMap<>();
		sortPropertyMapping.put("reportedAt", "latestReportedAt");
		sortPropertyMapping.put("reportCount", "reportCount");

		Sort newSort = Sort.unsorted();

		// Pageable의 Sort 객체 순회하며 개별 정렬 Order 처리
		for (Sort.Order order : pageable.getSort()) {
			String property = order.getProperty();
			Sort.Direction direction = order.getDirection();

			String sqlProperty = sortPropertyMapping.get(property);

			// 허용되지 않은 정렬 속성이면 오류 발생
			if (sqlProperty == null) {
				throw new ServiceException("400", "부적절한 정렬 옵션입니다.");
			}

			newSort = newSort.and(Sort.by(direction, sqlProperty));
		}

		// 만약 Pageable에 정렬 정보가 전혀 없었다면 기본 정렬 적용
		if (!newSort.isSorted()) {
			newSort = Sort.by(Sort.Direction.DESC, "latestReportedAt");
		}

		// Repository에 전달할 최종 Pageable 객체 생성 시 Unpaged Pageable 처리
		Pageable repositoryPageable;

		if (pageable.isPaged()) {
			repositoryPageable = PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				newSort
			);
		} else {
			// 입력 Pageable이 페이징 정보를 가지고 있지 않다면 (Unpaged)
			repositoryPageable = PageRequest.of(0, Integer.MAX_VALUE, newSort);
		}

		// 2. 검색 조건 검증 및 Repository 쿼리 파라미터에 맞게 매핑
		String searchTitle = null;
		String searchSummary = null;
		String searchKeyword = null;
		String searchReportReason = null;

		// 검색 대상과 검색어가 모두 존재하고, 검색어가 공백만으로 이루어지지 않았다면 매핑 로직 실행
		if (searchTarget != null && searchValue != null && !searchValue.trim().isEmpty()) {
			String trimmedSearchTarget = searchTarget.trim().toLowerCase();
			String trimmedSearchValue = searchValue.trim();

			// 검색 대상 문자열을 Repository 메서드의 인자로 매핑
			switch (trimmedSearchTarget) {
				case "post_title" -> searchTitle = trimmedSearchValue;
				case "post_summary" -> searchSummary = trimmedSearchValue;
				case "keyword" -> searchKeyword = trimmedSearchValue;
				case "report_reason" -> searchReportReason = trimmedSearchValue;
				default -> throw new ServiceException("400", "부적절한 검색 옵션입니다.");
			}
		}

		// 3. PostReportRepository 메서드 호출 및 반환
		return postReportRepository.findReportedPostSummary(
			searchTitle,
			searchSummary,
			searchKeyword,
			searchReportReason,
			repositoryPageable
		);
	}

	/**
	 * 관리자용 신고된 포스트들을 소프트 삭제(숨김) 처리합니다.
	 * @param postIds 숨길 포스트 ID 목록
	 */
	@Transactional
	public void hideReportedPost(List<Long> postIds) {
		// 1. 요청된 ID 목록이 비어있는지 확인
		if (postIds == null || postIds.isEmpty()) {
			throw new ServiceException("400", "삭제할 포스트 ID가 제공되지 않았습니다.");
		}

		// 2. 요청된 모든 포스트 ID에 해당하는 Post 엔티티들이 실제로 존재하는지 확인
		List<Post> existingPosts = postRepository.findAllById(postIds);
		if (existingPosts.size() != postIds.size()) {
			throw new ServiceException("404", "존재하지 않는 포스트가 포함되어 있습니다.");
		}

		// 3. 요청된 postIds 중 실제로 신고된 포스트의 개수를 확인
		long reportedPostCount = postReportRepository.countByPostIdIn(postIds);

		// 4. 요청된 postIds의 개수와 실제로 신고된 포스트의 개수가 다르면 에러 처리
		if (reportedPostCount != postIds.size()) {
			throw new ServiceException("400", "신고되지 않은 포스트가 요청에 포함되어 있습니다.");
		}

		// 5. 각 포스트를 숨김 처리 및 신고 상태 변경
		for (Long postId : postIds) {
			Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ServiceException("404", "내부 오류: 포스트를 찾을 수 없습니다."));

			// 이미 삭제된 포스트인지 확인
			if (post.isDeleted()) {
				throw new ServiceException("400", "ID [" + postId + "]포스트는 이미 삭제되었습니다.");
			}

			// 포스트를 숨김 처리
			post.softDelete();
			postRepository.save(post);
		}

		// 요청된 포스트 ID들에 해당하는 모든 신고 엔티티의 상태를 ACCEPTED로 업데이트
		postReportRepository.updateStatusByPostIdIn(postIds, ReportProcessingStatus.ACCEPTED);
	}

	/**
	 * 관리자용 신고된 포스트들의 신고를 거부(삭제) 처리합니다.
	 * @param postIds 신고를 거부할 포스트 ID 목록
	 */
	@Transactional
	public void rejectReportedPost(List<Long> postIds) {
		// 1. 요청된 ID 목록이 비어있는지 확인
		if (postIds == null || postIds.isEmpty()) {
			throw new ServiceException("400", "신고 거부할 포스트 ID가 제공되지 않았습니다.");
		}

		// 2. 요청된 모든 포스트 ID에 해당하는 Post 엔티티들이 실제로 존재하는지 확인
		List<Post> existingPosts = postRepository.findAllById(postIds);
		if (existingPosts.size() != postIds.size()) {
			throw new ServiceException("404", "존재하지 않는 포스트가 포함되어 있습니다.");
		}

		// 3. 요청된 postIds 중 실제로 신고된 포스트의 개수를 확인
		long reportedPostCount = postReportRepository.countByPostIdIn(postIds);

		// 4. 요청된 postIds의 개수와 실제로 신고된 포스트의 개수가 다르면 에러 처리
		if (reportedPostCount != postIds.size()) {
			throw new ServiceException("400", "신고되지 않은 포스트가 요청에 포함되어 있습니다. 신고된 포스트만 거부할 수 있습니다.");
		}

		// 5. 모든 검증을 통과했다면, 요청된 포스트 ID들에 해당하는 모든 신고 엔티티의 상태를 변경
		postReportRepository.updateStatusByPostIdIn(postIds, ReportProcessingStatus.REJECTED);
	}
}
