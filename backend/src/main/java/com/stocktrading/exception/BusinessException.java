package com.stocktrading.exception;

/** 业务异常。抛出后由 GlobalExceptionHandler 统一转成 ApiResponse。 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }

    public static BusinessException insufficientFunds(String message) {
        return new BusinessException(ErrorCode.INSUFFICIENT_FUNDS, message);
    }

    public static BusinessException insufficientPosition(String message) {
        return new BusinessException(ErrorCode.INSUFFICIENT_POSITION, message);
    }
}
