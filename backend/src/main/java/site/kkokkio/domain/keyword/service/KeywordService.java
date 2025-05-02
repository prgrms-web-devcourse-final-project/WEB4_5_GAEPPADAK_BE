package site.kkokkio.domain.keyword.service;

import org.springframework.dao.DataIntegrityViolationException;
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
import site.kkokkio.global.exception.ServiceException;

@Service
@RequiredArgsConstructor
public class KeywordService {
	private final KeywordRepository keywordRepository;
	private final PostKeywordRepository postKeywordRepository;

	@Transactional
	public Keyword createKeyword(Keyword keyword) {
		try {
			return keywordRepository.save(keyword);
		} catch (DataIntegrityViolationException e) {
			// Unique 제약 조건 위반 (이미 존재하는 키워드)
			return keywordRepository.findKeywordByText(keyword.getText())
				.orElseThrow(() -> new ServiceException("500", "키워드 저장 실패 및 조회 실패"));
		}
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
