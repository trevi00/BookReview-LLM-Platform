package com.bookreview.security;

import com.bookreview.domain.User;
import com.bookreview.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndIsActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        return createUserPrincipal(user);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return createUserPrincipal(user);
    }

    private UserPrincipal createUserPrincipal(User user) {
        List<GrantedAuthority> authorities = getAuthorities(user);
        
        return UserPrincipal.create(user, authorities);
    }

    private List<GrantedAuthority> getAuthorities(User user) {
        // 현재는 기본 USER 권한만 부여
        // 향후 역할 기반 권한 시스템으로 확장 가능
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static class UserPrincipal implements UserDetails {
        private Long id;
        private String username;
        private String email;
        private String password;
        private Collection<? extends GrantedAuthority> authorities;
        private boolean enabled;

        public UserPrincipal(Long id, String username, String email, String password,
                            Collection<? extends GrantedAuthority> authorities, boolean enabled) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.password = password;
            this.authorities = authorities;
            this.enabled = enabled;
        }

        public static UserPrincipal create(User user, Collection<? extends GrantedAuthority> authorities) {
            return new UserPrincipal(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getPassword(),
                    authorities,
                    user.getIsActive()
            );
        }

        public Long getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        @Override
        public String getUsername() {
            return email; // 이메일을 username으로 사용
        }

        public String getDisplayName() {
            return username; // 실제 사용자 이름
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
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
            return enabled;
        }
    }
}