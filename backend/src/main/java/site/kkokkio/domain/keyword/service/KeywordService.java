package site.kkokkio.domain.keyword.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.entity.PostKeyword;
import site.kkokkio.domain.post.repository.PostKeywordRepository;

@Service
@RequiredArgsConstructor
public class KeywordService {
	private final KeywordRepository keywordRepository;
	private final PostKeywordRepository postKeywordRepository;

	@Transactional
	public Keyword createKeyword(Keyword keyword) {
		return keywordRepository.findKeywordByText(keyword.getText())
			.orElseGet(() -> keywordRepository.save(Keyword.builder().text(keyword.getText()).build()));
	}

	@Transactional(readOnly = true)
	public Page<PostDto> getPostListByKeyword(String keywordText, Pageable pageable) {
		Page<PostKeyword> postKeywordPage = postKeywordRepository.findByKeywordTextWithPostAndKeyword(keywordText, pageable);
		if(postKeywordPage.isEmpty()) {
			return Page.empty();
		}
		return postKeywordPage.map(postKeyword ->
			PostDto.from(postKeyword.getPost(), postKeyword.getKeyword().getText()));
	}
}
