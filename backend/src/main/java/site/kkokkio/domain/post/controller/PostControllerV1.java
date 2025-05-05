package site.kkokkio.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.kkokkio.domain.post.controller.dto.PostDetailResponse;
import site.kkokkio.domain.post.controller.dto.TopPostResponse;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Post API V1", description = "포스트 관련 API 엔드포인트 V1")
public class PostControllerV1 {
	private final PostService postService;

	@Operation(summary = "포스트 조회")
	@ApiErrorCodeExamples({ErrorCode.POST_NOT_FOUND_2})
	@GetMapping("/{postId}")
	public RsData<PostDetailResponse> getPostById(@PathVariable Long postId) {
		PostDto postDto = postService.getPostWithKeywordById(postId);
		PostDetailResponse data = PostDetailResponse.builder()
			.postId(postDto.postId())
			.keyword(postDto.keyword())
			.title(postDto.title())
			.summary(postDto.summary())
			.thumbnailUrl(postDto.thumbnailUrl())
			.build();

		return new RsData<>(
			"200",
			"정상적으로 호출되었습니다.",
			data
		);
	}

	@Operation(summary = "실시간 키워드에 해당하는 포스트 리스트 조회")
	@ApiErrorCodeExamples({ErrorCode.POST_NOT_FOUND_2})
	@GetMapping("/top")
	public RsData<List<TopPostResponse>> getTopKeywordPosts() throws IOException {
		List<PostDto> postDtos = postService.getTopPostsWithKeyword();
		List<TopPostResponse> data = postDtos.stream()
			.map(dto -> TopPostResponse.builder()
				.postId(dto.postId())
				.keyword(dto.keyword())
				.title(dto.title())
				.summary(dto.summary())
				.thumbnailUrl(dto.thumbnailUrl())
				.build())
			.toList();

		return new RsData<>(
			"200",
			"정상적으로 호출되었습니다.",
			data
		);
	}

}
