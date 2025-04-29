package site.kkokkio.domain.post.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.global.exception.ServiceException;

@Service
@RequiredArgsConstructor
public class PostService {
	private final PostRepository postRepository;

	public Post getPostById(Long id) {
		return postRepository.findById(id)
			.orElseThrow(() -> new ServiceException("404", "해당 포스트를 찾을 수 없습니다."));
	}
}
