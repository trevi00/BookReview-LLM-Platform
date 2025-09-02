package com.bookreview.security;

import com.bookreview.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Spring Security의 UserDetails와 OAuth2User를 구현한 사용자 주체 클래스
 */
public class UserPrincipal implements OAuth2User, UserDetails {

    private Long id;
    private String email;
    private String username;
    @JsonIgnore
    private String password;
    private String profileImage;
    private boolean isActive;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public UserPrincipal(Long id, String email, String username, String password, 
                        String profileImage, boolean isActive, 
                        Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.profileImage = profileImage;
        this.isActive = isActive;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user, Collection<? extends GrantedAuthority> authorities) {
        return new UserPrincipal(
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getPassword(),
            user.getProfileImage(),
            user.getIsActive(),
            authorities
        );
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes, 
                                     Collection<? extends GrantedAuthority> authorities) {
        UserPrincipal userPrincipal = create(user, authorities);
        userPrincipal.setAttributes(attributes);
        return userPrincipal;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    // UserDetails interface methods
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // OAuth2User interface methods
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}