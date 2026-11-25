package com.example.healthexam.security;

import lombok.Data;

/** 登录态（从 JWT 还原），放在 ThreadLocal 中供 service 做数据范围校验 */
@Data
public class LoginUser {
    private Long userId;
    private String username;
    private String roleCode;
    private Long orgId;
}
