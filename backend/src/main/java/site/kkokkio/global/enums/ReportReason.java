package site.kkokkio.global.enums;

import java.util.Arrays;

public enum ReportReason {
	BAD_CONTENT("부적절한 내용"),
	RUDE_LANGUAGE("욕설/비방"),
	SPAM("스팸"),
	FALSE_INFO("허위 정보"),
	ETC("기타");

	private final String description;

	ReportReason(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * 한글 설명에 해당하는 ReportReason Enum을 찾아 반환합니다.
	 *
	 * @param description 검색할 한글 설명
	 * @return ReportReason Enum의 이름 (String), 없으면 null
	 */
	public static String findByNameOrDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}

		String lowerCaseDescription = description.trim().toLowerCase();

		return Arrays.stream(ReportReason.values())
			.filter(reason -> reason.name().toLowerCase().contains(lowerCaseDescription) ||
							  reason.description.toLowerCase().contains(lowerCaseDescription))
			.map(ReportReason::name)
			.findFirst()
			.orElse(null);
	}
}
