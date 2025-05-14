package site.kkokkio.domain.comment.dto;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.global.dto.PaginationMeta;

@Builder
public record ReportedCommentListResponse(
	@NonNull List<ReportedCommentResponse> list,
	@NonNull PaginationMeta meta
) {
	public static ReportedCommentListResponse from(Page<ReportedCommentSummary> service) {
		// 서비스 DTO 페이지의 내용을 항목 응답 DTO 리스트로 반환
		List<ReportedCommentResponse> dtoList = service.getContent().stream()
			.map(ReportedCommentResponse::from)
			.collect(Collectors.toList());

		// 페이지 객체의 정보를 사용하여 PaginationMeta 생성
		PaginationMeta paginationMeta = PaginationMeta.of(
			service.getNumber(),
			service.getSize(),
			service.getTotalElements(),
			service.getTotalPages(),
			service.hasNext(),
			service.hasPrevious()
		);

		// 최종 응답 DTO 생성 및 반환
		return ReportedCommentListResponse.builder()
			.list(dtoList)
			.meta(paginationMeta)
			.build();
	}
}
