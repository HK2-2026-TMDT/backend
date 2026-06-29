package vn.io.sanmaymac.modules.auth.dto;

public record AuthResponseRecord(
	String accessToken,
	String tokenType,
	long expiresIn,
	Long userId,
	String role,
	String refreshToken) {
}
