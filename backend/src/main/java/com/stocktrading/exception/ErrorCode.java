package com.stocktrading.exception;

/** 业务错误码。前端按 code 做分支，message 直接展示给用户。 */
public final class ErrorCode {

    private ErrorCode() {
    }

    /** 参数校验失败 */
    public static final int BAD_REQUEST = 1001;
    /** 登录名已存在 */
    public static final int LOGIN_NAME_TAKEN = 1002;
    /** 用户名或密码错误 */
    public static final int BAD_CREDENTIALS = 1003;
    /** 未登录或 token 失效 */
    public static final int UNAUTHORIZED = 1004;
    /** 无权操作该资源 */
    public static final int FORBIDDEN = 1005;

    /** 资金不足 */
    public static final int INSUFFICIENT_FUNDS = 2001;
    /** 可用股票不足 */
    public static final int INSUFFICIENT_POSITION = 2002;
    /** 订单不存在 */
    public static final int ORDER_NOT_FOUND = 2003;
    /** 订单不可撤销（已撤销或已全部成交） */
    public static final int ORDER_NOT_CANCELABLE = 2004;
    /** 股票不存在 */
    public static final int STOCK_NOT_FOUND = 2005;
    /** 用户不存在 */
    public static final int USER_NOT_FOUND = 2006;

    /** 服务器内部错误 */
    public static final int INTERNAL_ERROR = 5000;
}
