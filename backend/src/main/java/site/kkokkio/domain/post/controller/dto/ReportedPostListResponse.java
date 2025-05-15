package site.kkokkio.domain.post.controller.dto;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.post.dto.ReportedPostSummary;
import site.kkokkio.global.dto.PaginationMeta;

@Builder
public record ReportedPostListResponse(
	@NonNull List<ReportedPostResponse> list,
	@NonNull PaginationMeta meta
) {
	// Page<ReportedPostSummary>를 받아 ReportedPostListResponse DTO 생성
	public static ReportedPostListResponse from(Page<ReportedPostSummary> summaryPage) {
		// Page<ReportedPostSummary>의 내용을 List<ReportedPostResponse>로 매핑
		List<ReportedPostResponse> responseList = summaryPage.getContent().stream()
			.map(ReportedPostResponse::from)
			.collect(Collectors.toList());

		// Page 객체의 정보를 사용하여 PaginationMeta 생성
		PaginationMeta paginationMeta = PaginationMeta.of(
			summaryPage.getNumber(),
			summaryPage.getSize(),
			summaryPage.getTotalElements(),
			summaryPage.getTotalPages(),
			summaryPage.hasNext(),
			summaryPage.hasPrevious()
		);

		// ReportedPostListResponse DTO 생성 및 반환
		return ReportedPostListResponse.builder()
			.list(responseList)
			.meta(paginationMeta)
			.build();
	}
}
