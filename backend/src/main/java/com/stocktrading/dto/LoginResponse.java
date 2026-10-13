package com.stocktrading.dto;

/** 登录成功返回的凭证与用户信息。 */
public class LoginResponse {

    private String token;
    private Long userId;
    private String name;
    private String loginName;
    private Integer type;

    public LoginResponse() {
    }

    public LoginResponse(String token, Long userId, String name, String loginName, Integer type) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.loginName = loginName;
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
