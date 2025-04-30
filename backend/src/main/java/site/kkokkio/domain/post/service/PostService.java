package site.kkokkio.domain.post.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostKeyword;
import site.kkokkio.domain.post.entity.PostMetricHourly;
import site.kkokkio.domain.post.repository.PostKeywordRepository;
import site.kkokkio.domain.post.repository.PostMetricHourlyRepository;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;

@Service
@RequiredArgsConstructor
public class PostService {
	private final PostRepository postRepository;
	private final KeywordRepository keywordRepository;
	private final KeywordMetricHourlyRepository keywordMetricHourlyRepository;
	private final PostKeywordRepository postKeywordRepository;
	private final PostMetricHourlyRepository postMetricHourlyRepository;

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

	//스케쥴러에서 사용할 createPost 메서드
	@Transactional
	public Post createPost(Long keywordId, LocalDateTime bucketAt, String title, String summary, String thumbnailUrl) {

		// 1. Post 저장
		Post post = postRepository.save(
			Post.builder()
				.title(title)
				.summary(summary)
				.thumbnailUrl(thumbnailUrl)
				.bucketAt(bucketAt)
				.reportCount(0)
				.build()
		);

		// 2. KeywordMetricHourly 찾기(keywordId + bucketAt 둘 다 맞춰야 함)
		KeywordMetricHourlyId keywordMetricHourlyId = new KeywordMetricHourlyId(bucketAt, Platform.GOOGLE_TREND,
			keywordId);
		KeywordMetricHourly keywordMetricHourly = keywordMetricHourlyRepository.findById(keywordMetricHourlyId)
			.orElseThrow(() -> new ServiceException("404", "KeywordMetricHourly를 찾을 수 없습니다."));

		// 3. Keyword 찾기
		Keyword keyword = keywordRepository.findById(keywordId)
			.orElseThrow(() -> new ServiceException("404", "Keyword를 찾을 수 없습니다."));

		// 4. KeywordMetricHourly에 post 업데이트
		keywordMetricHourly.setPost(post);

		// 5. PostKeyword 저장
		PostKeyword postKeyword = PostKeyword.builder()
			.post(post)
			.keyword(keyword)
			.build();
		postKeywordRepository.save(postKeyword);

		// 6. PostMetricHourly 저장
		PostMetricHourly postMetricHourly = PostMetricHourly.builder()
			.post(post)
			.bucketAt(bucketAt)
			.clickCount(0)
			.likeCount(0)
			.build();
		postMetricHourlyRepository.save(postMetricHourly);

		return post;
	}
}
