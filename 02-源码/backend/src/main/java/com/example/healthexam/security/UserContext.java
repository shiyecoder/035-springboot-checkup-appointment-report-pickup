package com.example.healthexam.security;

/** 当前请求的登录用户上下文；请求结束必须 clear，避免线程池串号 */
public class UserContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static String role() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getRoleCode();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
