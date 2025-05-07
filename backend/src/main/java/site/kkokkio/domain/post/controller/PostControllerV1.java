package site.kkokkio.domain.post.controller;

import java.io.IOException;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.post.controller.dto.PostSearchSourceListResponse;
import site.kkokkio.domain.keyword.service.KeywordService;
import site.kkokkio.domain.post.controller.dto.PostDetailResponse;
import site.kkokkio.domain.post.controller.dto.TopPostResponse;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.dto.PostListResponse;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Post API V1", description = "포스트 관련 API 엔드포인트 V1")
public class PostControllerV1 {
	private final PostService postService;
	private final KeywordService keywordService;
	private final SourceService sourceService;

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

	@GetMapping("/search")
	@ApiErrorCodeExamples({ErrorCode.POST_NOT_FOUND_3})
	@Operation(summary = "키워드 기반 Post 검색")
	public RsData<PostListResponse> getPostListByKeyword(
		@RequestParam String keyword,
		@RequestParam(value = "sort", required = false, defaultValue = "createdAt") String sortField, // 정렬할 필드
		@RequestParam(value = "order", required = false, defaultValue = "DESC") Sort.Direction orderDirection, // 정렬 방향 필드 추가
		@ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable defaultPageable
	) {
		// 제목 정렬 시 PostKeyword 엔티티에 title이 없으므로 post.title로 접근
		if(sortField.equals("title"))
			sortField = "post.title";

		Sort sort = Sort.by(orderDirection, sortField);
		Pageable customPaging = PageRequest.of(
			defaultPageable.getPageNumber(),
			defaultPageable.getPageSize(),
			sort
		);

		Page<PostDto> postDtoList = keywordService.getPostListByKeyword(keyword, customPaging);
		PostListResponse response = PostListResponse.from(postDtoList);
		return new RsData<>(
			"200",
			"키워드의 포스트를 불러왔습니다.",
			response
		);
	}

	@Operation(
		summary = "키워드 검색 출처 5개 최신순 조회 (페이지네이션)",
		description = "키워드 검색 결과인 포스트 목록 기준으로 최신순 출처 목록을 조회힙니다. (없을 경우 빈 배열 반환)"
	)
	@ApiErrorCodeExamples({})
	@GetMapping("/search/sources")
	public RsData<PostSearchSourceListResponse> getKeywordSearchSources(
		@RequestParam String keyword,
		@RequestParam(value = "sort", required = false, defaultValue = "createdAt") String sortField, // 정렬할 필드
		@RequestParam(value = "order", required = false, defaultValue = "DESC") Sort.Direction orderDirection, // 정렬 방향 필드 추가
		@ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable defaultPageable
	) {
		if(sortField.equals("title"))
		sortField = "post.title";
		
		Sort sort = Sort.by(orderDirection, sortField);
		Pageable customPaging = PageRequest.of(
			defaultPageable.getPageNumber(),
			defaultPageable.getPageSize(),
			sort
		);
	    	Page<PostDto> postDtoList = keywordService.getPostListByKeyword(keyword, customPaging);
	    	List<SourceDto> searchSourceList = sourceService.getTop5SourcesByPosts(postDtoList.toList());
	    	PostSearchSourceListResponse response = PostSearchSourceListResponse.from(searchSourceList, postDtoList);
	    	return new RsData<>(
	      		"200",
	      		"성공적으로 조회되었습니다.",
	      		response
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
