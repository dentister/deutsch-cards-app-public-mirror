package com.kniazev.cards.word.db.model;

import org.springframework.security.core.GrantedAuthority;

import com.kniazev.cards.word.constant.Roles;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Authority implements GrantedAuthority {
    public static final Authority ADMIN_AUTHORITY = new Authority(Roles.ROLE_ADMIN);
    public static final Authority USER_AUTHORITY = new Authority(Roles.ROLE_USER);
    
    private final String name;

    @Override
    public String getAuthority() {
        return name;
    }
    
}
