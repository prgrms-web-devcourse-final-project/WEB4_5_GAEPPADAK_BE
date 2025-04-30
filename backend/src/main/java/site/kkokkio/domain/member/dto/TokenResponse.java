package site.kkokkio.domain.member.dto;

public record TokenResponse(String accessToken, String refreshToken) {
}