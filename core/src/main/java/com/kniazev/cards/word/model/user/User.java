package com.kniazev.cards.word.model.user;

import org.hibernate.annotations.Type;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "users")
@SequenceGenerator(name= "security_seq_gen", sequenceName = "security_seq", initialValue=1, allocationSize = 1)
public class User implements UserDetails {
    
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="security_seq_gen")
    private Long id;
    
    @Column(name = "username")
    private String username;

    @Column(name = "telegram_id")
    private Long telegramId;

    @Column(name = "password")
    private String password;
    
    @Transient
    private String email;
    
    @Column(name = "enabled")
    private boolean enabled;
    
    @Column(name = "roles", columnDefinition = "text[]")
    @Type(value = ListArrayType.class)
    private List<String> roles;
    
    @Builder.Default
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserSettings userSettings = new UserSettings();
    
    public UserSettings getUserSettings() {
        if (userSettings == null) {
            userSettings = new UserSettings();
            userSettings.setUser(this);
        }
        
        return userSettings;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}