package com.stocktrading.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户。资金分「可用」与「冻结」两部分：
 * 下单买入时把 price*amount 从 cnyFree 划到 cnyFrozen，成交/撤单时再结算。
 */
@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_login_name", columnList = "login_name", unique = true))
public class User {

    /** 普通用户 */
    public static final int TYPE_NORMAL = 0;
    /** 管理员 */
    public static final int TYPE_ADMIN = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "login_name", nullable = false, unique = true, length = 64)
    private String loginName;

    /** BCrypt 哈希，不存明文 */
    @Column(nullable = false, length = 128)
    private String passwd;

    @Column(nullable = false)
    private Integer type = TYPE_NORMAL;

    /** 可用人民币 */
    @Column(name = "cny_free", nullable = false, precision = 19, scale = 4)
    private BigDecimal cnyFree = BigDecimal.ZERO;

    /** 冻结人民币 */
    @Column(name = "cny_frozen", nullable = false, precision = 19, scale = 4)
    private BigDecimal cnyFrozen = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isAdmin() {
        return type != null && type == TYPE_ADMIN;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public BigDecimal getCnyFree() {
        return cnyFree;
    }

    public void setCnyFree(BigDecimal cnyFree) {
        this.cnyFree = cnyFree;
    }

    public BigDecimal getCnyFrozen() {
        return cnyFrozen;
    }

    public void setCnyFrozen(BigDecimal cnyFrozen) {
        this.cnyFrozen = cnyFrozen;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
