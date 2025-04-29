package site.kkokkio.domain.post.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.global.dto.RsData;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Post API V1", description = "포스트 관련 API 엔드포인트 V1")
public class PostControllerV1 {
	private final PostService postService;

	@GetMapping("/{postId}")
	public RsData<PostDto> getPostById(@PathVariable Long postId) {
		PostDto data = postService.getPostWithKeywordById(postId);
		return new RsData<>(
			"200",
			"정상적으로 호출되었습니다.",
			data
		);
	}

	@GetMapping("/top")
	public RsData<List<PostDto>> getTopKeywordPosts() {
		List<PostDto> data = postService.getTopPostsWithKeyword();
		return new RsData<>(
			"200",
			"정상적으로 호출되었습니다.",
			data
		);
	}


}
