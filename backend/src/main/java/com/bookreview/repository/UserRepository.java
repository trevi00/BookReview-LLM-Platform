package com.bookreview.repository;

import com.bookreview.domain.User;
import com.bookreview.domain.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일로 활성 사용자 조회
     */
    Optional<User> findByEmailAndIsActiveTrue(String email);

    /**
     * 사용자명으로 사용자 조회
     */
    Optional<User> findByUsername(String username);

    /**
     * OAuth 제공자와 제공자 ID로 사용자 조회
     */
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 사용자명 존재 여부 확인
     */
    boolean existsByUsername(String username);

    /**
     * 활성 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();

    /**
     * OAuth 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.provider != 'LOCAL' AND u.isActive = true")
    long countOAuthUsers();

    /**
     * 특정 기간 내 가입한 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate AND u.createdAt <= :endDate")
    long countUsersByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate, 
                                     @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * ID로 활성 사용자 조회
     */
    Optional<User> findByIdAndIsActiveTrue(Long id);
}