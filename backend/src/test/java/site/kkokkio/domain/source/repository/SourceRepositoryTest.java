package site.kkokkio.domain.source.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostKeyword;
import site.kkokkio.domain.post.repository.PostKeywordRepository;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.global.enums.Platform;

@SpringBootTest
@Transactional
class SourceRepositoryTest {


    @Autowired
	private SourceRepository       sourceRepo;
    @Autowired private PostRepository postRepo;
    @Autowired private KeywordRepository keywordRepo;
    @Autowired private PostKeywordRepository postKeywordRepo;
    @Autowired private PostSourceRepository   postSourceRepo;

	@Test
	@DisplayName("플랫폼과 키워드 필터링 Query 검증 - 성공")
	void findLatest10ByPlatformAndKeyword() {
        // given
        Keyword kw = keywordRepo.save(Keyword.builder().text("test").build());
        Post post = postRepo.save(Post.builder()
			.title("Test Post")
			.bucketAt(LocalDateTime.now())
			.summary("summary")
			.build());
        postKeywordRepo.save(PostKeyword.builder().post(post).keyword(kw).build());

        Source oldest  = sourceRepo.save(
            Source.builder()
                  .normalizedUrl("url1")
                  .thumbnailUrl(null)
                  .title("old")
                  .description("")
                  .publishedAt(LocalDateTime.now().minusDays(3))
                  .platform(Platform.NAVER_NEWS)
                  .build()
        );
        Source newest   = sourceRepo.save(
            Source.builder()
                  .normalizedUrl("url3")
                  .thumbnailUrl(null)
                  .title("new")
                  .description("")
                  .publishedAt(LocalDateTime.now().minusDays(1))
                  .platform(Platform.NAVER_NEWS)
                  .build()
        );
        Source middle  = sourceRepo.save(
            Source.builder()
                  .normalizedUrl("url2")
                  .thumbnailUrl(null)
                  .title("mid")
                  .description("")
                  .publishedAt(LocalDateTime.now().minusDays(2))
                  .platform(Platform.NAVER_NEWS)
                  .build()
        );

        postSourceRepo.save(PostSource.builder().post(post).source(oldest).build());
        postSourceRepo.save(PostSource.builder().post(post).source(middle).build());
        postSourceRepo.save(PostSource.builder().post(post).source(newest).build());

        // when
        List<Source> results = sourceRepo.findLatest10ByPlatformAndKeyword(
            Platform.NAVER_NEWS,
            "test",
            PageRequest.of(0, 10)
        );

        //--- then
        assertThat(results)
            .hasSize(3)
            .extracting(Source::getNormalizedUrl)
            .containsExactly("url3", "url2", "url1");
    }
}