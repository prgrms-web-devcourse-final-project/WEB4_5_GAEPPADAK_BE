package site.kkokkio.global.exception.doc;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
	LOGOUT_BAD_REQUEST("401", "로그인 상태가 아닙니다."),
	REFRESH_TOKEN_NOT_FOUND("401", "리프레시 토큰이 없습니다."),
	REFRESH_TOKEN_MISMATCH("401", "유효하지 않은 리프레시 토큰입니다."),
	MEMBER_ME_BAD_REQUEST("400", "마이페이지 조회 실패"),
	COMMENT_NOT_FOUND("404", "존재하지 않는 댓글입니다."),
	COMMENT_UPDATE_FORBIDDEN("403", "본인 댓글만 수정할 수 있습니다."),
	COMMENT_DELETE_FORBIDDEN("403", "본인 댓글만 삭제할 수 있습니다."),
	COMMENT_LIKE_FORBIDDEN("403", "본인 댓글은 좋아요 할 수 없습니다."),
	COMMENT_LIKE_BAD_REQUEST("400", "이미 좋아요를 누른 댓글입니다."),
	COMMENT_UNLIKE_BAD_REQUEST("400", "이미 좋아요가 취소된 상태입니다."),
	EMAIL_ALREADY_EXIST("409", "이미 사용중인 이메일입니다."),
	EMAIL_NOT_FOUND("404", "존재하지 않는 이메일입니다."),
	EMAIL_UNAUTHORIZED("401", "메일이 인증되지 않은 회원입니다."),
	AUTH_CODE_UNAUTHORIZED("401", "인증코드가 유효하지 않습니다."),
	NICKNAME_ALREADY_EXIST("409", "이미 사용중인 닉네임입니다."),
	PASSWORD_UNAUTHORIZED("401", "비밀번호가 올바르지 않습니다."),
	POST_NOT_FOUND_1("404", "해당 포스트를 찾을 수 없습니다."),
	POST_NOT_FOUND_2("404", "포스트를 불러오지 못했습니다."),
	POST_NOT_FOUND_3("404", "존재하지 않는 포스트입니다."),
	KEYWORD_METRIC_HOURLY_NOT_FOUND("404", "KeywordMetricHourly를 찾을 수 없습니다."),
	KEYWORDS_NOT_FOUND_1("404", "키워드를 불러오지 못했습니다."),
	KEYWORDS_NOT_FOUND_2("404", "Keyword를 찾을 수 없습니다."),
	NAVER_BAD_GATEWAY("502", "Empty response from Naver News API"),
	MISSING_TOKEN("401", "인증 토큰이 없어 인증 실패"),
	TOKEN_EXPIRED("401", "토큰이 만료되어 인증할 수 없음"),
	UNSUPPORTED_TOKEN("400", "지원하지 않는 토큰 형식"),
	MALFORMED_TOKEN("400", "클라이언트가 보낸 토큰 자체가 형식적으로 잘못됨"),
	CREDENTIALS_MISMATCH("401", "토큰 서명 불일치 등으로 인증 실패"),
	SOURCE_NOT_FOUND_1("404", "유튜브 비디오를 불러오지 못했습니다."),
	SOURCE_NOT_FOUND_2("404", "뉴스를 불러오지 못했습니다."),
	REPORT_REASON_BAD_REQUEST("400", "신고 이유를 선택해주세요."),
	REPORT_COMMENT_BAD_REQUEST("400", "댓글 신고에 실패했습니다."),
	REPORT_COMMENT_DUPLICATE("400", "이미 신고한 댓글입니다."),
	REPORT_COMMENT_FORBIDDEN("403", "본인의 댓글은 신고할 수 없습니다."),
	REPORT_POST_BAD_REQUEST("400", "포스트 신고에 실패했습니다."),
	REPORT_POST_DUPLICATE("400", "이미 신고한 포스트입니다."),
	FORBIDDEN("403", "권한이 없습니다."),
	BAD_SEARCH_OPTION("400", "부적절한 검색 옵션입니다."),
	BAD_SORT_OPTION("400", "부적절한 정렬 옵션입니다."),
	COMMENT_IDS_NOT_PROVIDED("400", "댓글이 선택되지 않았습니다."),
	COMMENT_NOT_INCLUDE("404", "존재하지 않는 댓글이 포함되어 있습니다."),
	MEMBER_NOT_FOUND("404", "사용자를 찾을 수 없습니다.");

	private final String code;
	private final String message;
}
