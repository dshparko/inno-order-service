package com.innowise.orderservice.config;

/**
 * @ClassName AuthConstant
 * @Description Represents constant values used across authentication and security components.
 * @Author dshparko
 * @Date 08.10.2025 16:11
 * @Version 1.0
 */
public final class AuthConstant {
    private AuthConstant() {
    }

    public static final String IDS_PARAM = "ids";
    public static final String SLASH = "/";
    public static final String QUERY_PREFIX = "?";
    public static final String PARAM_SEPARATOR = "&";
    public static final String KEY_VALUE_SEPARATOR = "=";
    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String CONTENT_TYPE = "application/json";
}
