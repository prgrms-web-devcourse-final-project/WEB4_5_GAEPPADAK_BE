package site.kkokkio.global.exception.doc;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
	COMMENT_NOT_FOUND("404", "존재하지 않는 댓글입니다."),
	COMMENT_UPDATE_FORBIDDEN("403", "본인 댓글만 수정할 수 있습니다."),
	COMMENT_DELETE_FORBIDDEN("403", "본인 댓글만 삭제할 수 있습니다."),
	COMMENT_LIKE_FORBIDDEN("403", "본인 댓글은 좋아요 할 수 없습니다."),
	COMMENT_LIKE_BAD_REQUEST("400", "이미 좋아요를 누른 댓글입니다."),
	EMAIL_ALREADY_EXIST("409-1", "이미 사용중인 이메일입니다."),
	EMAIL_NOT_FOUND("404", "존재하지 않는 이메일입니다."),
	NICKNAME_ALREADY_EXIST("409-2", "이미 사용중인 닉네임입니다."),
	PASSWORD_UNAUTHORIZED("401", "비밀번호가 올바르지 않습니다."),
	POST_NOT_FOUND_1("404", "해당 포스트를 찾을 수 없습니다."),
	POST_NOT_FOUND_2("404", "포스트를 불러오지 못했습니다."),
	POST_NOT_FOUND_3("404", "존재하지 않는 포스트입니다."),
	KEYWORD_METRIC_HOURLY_NOT_FOUND("404", "KeywordMetricHourly를 찾을 수 없습니다."),
	KEYWORDS_NOT_FOUND_1("404", "키워드를 불러오지 못했습니다."),
	KEYWORDS_NOT_FOUND_2("404", "Keyword를 찾을 수 없습니다."),
	NAVER_BAD_GATEWAY("502", "Empty response from Naver News API");

	private final String code;
	private final String message;
}
