package com.my;

/**
 * @author duoyian
 * @date 2026/7/20
 */
public class UserService {
    public String getUserInfo(String userId, int age) {
        return String.format("{\"userId\":\"%s\", \"age\":%d, \"status\":\"active\"}", userId, age);
    }
}
