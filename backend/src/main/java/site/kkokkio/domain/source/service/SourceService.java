package site.kkokkio.domain.source.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.global.enums.Platform;

@Service
@RequiredArgsConstructor
public class SourceService {

	private final PostSourceRepository postSourceRepository;
	private final PostService postService;

	public List<SourceDto> getTop10NewsSourcesByPostId(Long postId) {
		postService.getPostById(postId);
		Platform newsPlatform = Platform.NAVER_NEWS;
		PageRequest pageRequest = PageRequest.of(0, 10);
		List<PostSource> postSources = postSourceRepository.findAllWithSourceByPostIdAndPlatform(
			postId,
			newsPlatform,
			pageRequest);
		return postSources.stream()
			.map(ps -> SourceDto.from(ps.getSource()))
			.toList();
	}
}
