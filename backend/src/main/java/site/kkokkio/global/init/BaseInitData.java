package site.kkokkio.global.init;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.keyword.service.GoogleTrendsRssService;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostKeyword;
import site.kkokkio.domain.post.repository.PostKeywordRepository;
import site.kkokkio.domain.post.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class BaseInitData {

	private final PostRepository postRepository;
	private final PostKeywordRepository postKeywordRepository;
	private final GoogleTrendsRssService googleTrendsRssService;

	@PostConstruct
	@Transactional
	public void init() {
		if (postRepository.count() > 0) {
			return; // 이미 데이터가 존재하면 초기화하지 않음
		}

		List<Keyword> keywords = createKeywords();
		List<Post> posts = createPosts();

		// 포스트와 키워드 연결 (각 포스트에 하나의 키워드 연결)
		Random random = new Random();
		for (Post post : posts) {
			Keyword randomKeyword = keywords.get(random.nextInt(keywords.size()));
			PostKeyword postKeyword = PostKeyword.builder()
				.post(post)
				.keyword(randomKeyword)
				.build();
			postKeywordRepository.save(postKeyword);
		}
	}

	private List<Keyword> createKeywords() {
		List<Keyword> keywords = new ArrayList<>();
		keywords = googleTrendsRssService.getTrendingKeywordsFromRss();
		// 필요하다면 더 많은 키워드 추가
		return keywords;
	}

	private List<Post> createPosts() {
		List<Post> posts = new ArrayList<>();
		Random random = new Random();
		for (int i = 1; i <= 30; i++) {
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime bucketAt = now.minusDays(random.nextInt(30)).minusHours(random.nextInt(24)).minusMinutes(random.nextInt(60));

			Post post = Post.builder()
				.title("임시 포스트 " + i)
				.summary("임시 포스트 " + i + "의 요약 내용입니다.")
				.bucketAt(bucketAt)
				.reportCount(random.nextInt(10))
				.build();
			posts.add(post);
		}
		return postRepository.saveAll(posts);
	}
}