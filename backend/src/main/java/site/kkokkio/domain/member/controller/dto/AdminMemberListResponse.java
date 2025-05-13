package site.kkokkio.domain.member.controller.dto;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.NonNull;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.dto.PaginationMeta;

@Builder
public record AdminMemberListResponse(
	@NonNull List<AdminMemberDto> list,
	@NonNull PaginationMeta meta
) {
	// Page<Member> 객체를 받아 AdminMemberListResponse DTO를 생성하는 팩토리 메소드
	public static AdminMemberListResponse from(Page<Member> memberPage) {
		// Page<Member>의 내용을 List<AdminMemberDto>로 변환
		List<AdminMemberDto> adminMemberDtoList = memberPage.getContent().stream()
			.map(AdminMemberDto::from)
			.collect(Collectors.toList());

		// Page 객체의 정보를 사용하여 PaginationMeta 생성
		PaginationMeta paginationMeta = PaginationMeta.of(
			memberPage.getNumber(),
			memberPage.getSize(),
			memberPage.getTotalElements(),
			memberPage.getTotalPages(),
			memberPage.hasNext(),
			memberPage.hasPrevious()
		);

		// AdminMemberListResponse DTO 생성 및 반환
		return AdminMemberListResponse.builder()
			.list(adminMemberDtoList)
			.meta(paginationMeta)
			.build();
	}
}
