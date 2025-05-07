package site.kkokkio.domain.keyword.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostKeyword;
import site.kkokkio.domain.post.repository.PostKeywordRepository;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class KeywordServiceTest {
	@InjectMocks
	private KeywordService keywordService;

	@Mock
	private KeywordRepository keywordRepository;

	@Mock
	private PostKeywordRepository postKeywordRepository;

	@Test
	@DisplayName("키워드 생성 - 새로운 키워드")
	void createKeywordTest_NewKeyword(){
		// Given
		String keywordText = "새로운 키워드";
		Keyword newKeyword = Keyword.builder().text(keywordText).build();
		when(keywordRepository.findKeywordByText(keywordText)).thenReturn(Optional.empty());
		when(keywordRepository.save(any(Keyword.class))).thenReturn(newKeyword);

		// When
		Keyword createdKeyword = keywordService.createKeyword(newKeyword);

		// Then
		assertThat(createdKeyword.getText()).isEqualTo(keywordText);
	}

	@Test
	@DisplayName("키워드 생성 - 기존 키워드 (Unique 제약 조건 위반)")
	void createKeywordTest_ExistingKeyword_UniqueViolation(){
		// Given
		String keywordText = "기존 키워드";
		Keyword existingKeyword = Keyword.builder().text(keywordText).build();

		// 제약 조건 위반 시
		when(keywordRepository.save(any(Keyword.class))).thenThrow(DataIntegrityViolationException.class);
		when(keywordRepository.findKeywordByText(keywordText)).thenReturn(Optional.of(existingKeyword));

		// When
		Keyword createdKeyword = keywordService.createKeyword(existingKeyword);

		// Then
		assertThat(createdKeyword).isEqualTo(existingKeyword);
		verify(keywordRepository, times(1)).findKeywordByText(keywordText);
		verify(keywordRepository, never()).save(any());
	}

	@Test
	@DisplayName("키워드 조회 - 최신순 (기본)")
	void findKeywordTest_Default(){
		// Given
		String keywordText = "테스트 키워드";
		Pageable pageable = PageRequest.of(0, 10, Sort.by("post.createdAt").descending()); // 변경
		LocalDateTime now = LocalDateTime.now();
		List<PostKeyword> postKeywords = Arrays.asList(
			createPostKeyword(createPost(1L, "제목1", now), keywordText),
			createPostKeyword(createPost(2L, "제목2", now.minusDays(1)), keywordText),
			createPostKeyword(createPost(3L, "제목3", now.minusDays(2)), keywordText)
		);
		Page<PostKeyword> postKeywordPage = new PageImpl<>(postKeywords, pageable, postKeywords.size());
		when(postKeywordRepository.findByKeywordTextWithPostAndKeyword(keywordText, pageable)).thenReturn(postKeywordPage);

		// When
		Page<PostDto> postDtoPage = keywordService.getPostListByKeyword(keywordText, pageable);

		// Then
		assertThat(postDtoPage.getContent().get(0).postId()).isEqualTo(1L); // 최신순
		assertThat(postDtoPage.getContent().get(1).postId()).isEqualTo(2L);
		assertThat(postDtoPage.getContent().get(2).postId()).isEqualTo(3L);
	}

	@Test
	@DisplayName("키워드 조회 - 제목 오름차순")
	void findKeywordTest_TitleAsc() {
		// Given
		String keywordText = "테스트 키워드";
		Pageable pageable = PageRequest.of(0, 10, Sort.by("post.title").ascending()); // 변경
		List<PostKeyword> postKeywords = Arrays.asList(
			createPostKeyword(createPost(1L, "제목1", LocalDateTime.now().minusDays(2)), keywordText),
			createPostKeyword(createPost(2L, "제목2", LocalDateTime.now()), keywordText),
			createPostKeyword(createPost(3L, "제목3", LocalDateTime.now().minusDays(1)), keywordText)
		);
		// 제목 오름차순으로 정렬
		postKeywords.sort(Comparator.comparing(pk -> pk.getPost().getTitle()));
		Page<PostKeyword> postKeywordPage = new PageImpl<>(postKeywords, pageable, postKeywords.size());
		when(postKeywordRepository.findByKeywordTextWithPostAndKeyword(keywordText, pageable)).thenReturn(postKeywordPage);

		// When
		Page<PostDto> postDtoPage = keywordService.getPostListByKeyword(keywordText, pageable);

		// Then
		assertThat(postDtoPage.getContent().get(0).postId()).isEqualTo(1L); // 제목 오름차순
		assertThat(postDtoPage.getContent().get(1).postId()).isEqualTo(2L);
		assertThat(postDtoPage.getContent().get(2).postId()).isEqualTo(3L);
	}

	@Test
	@DisplayName("키워드 조회 - 포스트가 없는 경우 Page.empty() 반환")
	void findKeywordTest_NoPosts() {
		// Given
		String keywordText = "존재하지 않는 키워드";
		Pageable pageable = PageRequest.of(0, 10);
		Page<PostKeyword> emptyPage = Page.empty();
		when(postKeywordRepository.findByKeywordTextWithPostAndKeyword(keywordText, pageable)).thenReturn(emptyPage);

		// When
		Page<PostDto> postDtoPage = keywordService.getPostListByKeyword(keywordText, pageable);

		// Then
		assertThat(postDtoPage.isEmpty()).isTrue();
	}

	private Post createPost(Long id, String title, LocalDateTime createdAt) {
		return Post.builder()
			.id(id)
			.title(title)
			.summary("요약")
			.bucketAt(createdAt)
			.reportCount(0)
			.thumbnailUrl(null)
			.build();
	}

	private PostKeyword createPostKeyword(Post post, String keywordText) {
		return PostKeyword.builder()
			.post(post)
			.keyword(Keyword.builder().text(keywordText).build())
			.build();
	}
}
