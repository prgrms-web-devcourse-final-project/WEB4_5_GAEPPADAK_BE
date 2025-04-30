package site.kkokkio.domain.keyword.controller;


import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.service.KeywordService;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.dto.PostListResponse;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/keywords")
@Tag(name = "Keyword API", description = "Keyword 관련 API")
public class KeywordControllerV1 {

	private final KeywordService keywordService;

	@GetMapping("/search")
	@ApiErrorCodeExamples({ErrorCode.POST_NOT_FOUND_3})
	@Operation(summary = "키워드 기반 Post 검색")
	public RsData<PostListResponse> getPostListByKeyword(
		@RequestParam String keyword,
		@ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
		) {
		Page<PostDto> postDtoList = keywordService.getPostListByKeyword(keyword, pageable);
		PostListResponse response = PostListResponse.from(postDtoList);
		return new RsData<>(
			"200",
			"키워드의 포스트를 불러왔습니다.",
			response
		);
	}
}
