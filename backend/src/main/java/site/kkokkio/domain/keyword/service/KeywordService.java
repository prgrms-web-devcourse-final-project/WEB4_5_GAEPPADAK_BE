package site.kkokkio.domain.keyword.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.repository.PostKeywordRepository;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.global.exception.ServiceException;

@Service
@RequiredArgsConstructor
public class KeywordService {
	private final KeywordRepository keywordRepository;
	private final PostRepository postRepository;
	private final PostKeywordRepository postKeywordRepository;

	private static final int MAX_POST_COUNT = 10;

	@Transactional
	public Keyword createKeyword(Keyword keyword) {
		return keywordRepository.findKeywordByText(keyword.getText())
			.orElseGet(() -> keywordRepository.save(keyword));
	}

	@Transactional(readOnly = true)
	public Page<PostDto> getPostListByKeyword(String keywordText, Pageable pageable) {
		Page<Post> postPage = postRepository.findPostsByKeywordText(keywordText, pageable);
		if(postPage.isEmpty()) {
			throw new ServiceException("404", "포스트가 존재하지 않습니다.");
		}
		return postPage.map(post -> {
			return postKeywordRepository.findByPost_Id(post.getId()).stream()
				.findFirst()
				.map(postKeyword -> PostDto.from(post, postKeyword.getKeyword().getText()))
				.orElseGet(() -> PostDto.from(post, "")); // 연결된 키워드가 없을 경우 빈 문자열 처리
		});
	}
}
