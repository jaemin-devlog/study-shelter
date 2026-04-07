package likelion.simsim.common;

/**
 * REST 응답 형식을 통일하기 위한 공통 응답 객체입니다.
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
