package com.example.camunda_spring.service;

import org.camunda.bpm.engine.IdentityService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FcmTokenStore {

    private static final String FCM_TOKEN_USER_INFO_KEY = "fcm_token";

    private final IdentityService identityService;
    private final JdbcTemplate jdbcTemplate;

    public FcmTokenStore(IdentityService identityService, JdbcTemplate jdbcTemplate) {
        this.identityService = identityService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void saveToken(String userId, String token) {
        deleteToken(userId);
        identityService.setUserInfo(userId, FCM_TOKEN_USER_INFO_KEY, token);
    }

    @Transactional
    public String getToken(String userId) {
        List<String> tokens = jdbcTemplate.queryForList("""
                select VALUE_
                from ACT_ID_INFO
                where USER_ID_ = ?
                  and KEY_ = ?
                  and VALUE_ is not null
                order by ID_ desc
                """, String.class, userId, FCM_TOKEN_USER_INFO_KEY);

        if (tokens.isEmpty()) {
            return null;
        }

        String newestToken = tokens.get(0);
        if (tokens.size() > 1) {
            saveToken(userId, newestToken);
        }

        return newestToken;
    }

    public void deleteToken(String userId) {
        jdbcTemplate.update("""
                delete from ACT_ID_INFO
                where USER_ID_ = ?
                  and KEY_ = ?
                """, userId, FCM_TOKEN_USER_INFO_KEY);
    }
}
