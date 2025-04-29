package site.kkokkio.domain.keyword.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.repository.KeywordRepository;

@Service
@RequiredArgsConstructor
public class KeywordService {
	private final KeywordRepository keywordRepository;

	@Transactional
	public Keyword createKeyword(Keyword keyword) {
		return keywordRepository.findKeywordByText(keyword.getText())
			.orElseGet(() -> keywordRepository.save(keyword));
	}
}
