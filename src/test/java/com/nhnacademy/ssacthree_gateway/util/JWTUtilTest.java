package com.nhnacademy.ssacthree_gateway.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JWTUtilTest {

    private JWTUtil jwtUtil;
    private String secretKey;

    @BeforeEach
    void setUp() {
        // 테스트 환경에서 강력한 키를 생성
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        secretKey = new String(key.getEncoded());
        jwtUtil = new JWTUtil(secretKey); // JWTUtil 생성자에 동일한 키 전달
    }

    @Test
    void testGetMemberLoginId() {
        // JWT 생성
        String token = Jwts.builder()
            .setSubject("user")
            .claim("memberLoginId", "testUser123")
            .signWith(new SecretKeySpec(secretKey.getBytes(), SignatureAlgorithm.HS256.getJcaName()), SignatureAlgorithm.HS256)
            .compact();

        // JWTUtil을 통해 검증
        String memberLoginId = jwtUtil.getMemberLoginId(token);
        assertThat(memberLoginId).isEqualTo("testUser123");
    }

    @Test
    void testGetRole() {
        // JWT 생성
        String token = Jwts.builder()
            .setSubject("role")
            .claim("role", "ROLE_ADMIN")
            .signWith(new SecretKeySpec(secretKey.getBytes(), SignatureAlgorithm.HS256.getJcaName()), SignatureAlgorithm.HS256)
            .compact();

        // JWTUtil을 통해 검증
        String role = jwtUtil.getRole(token);
        assertThat(role).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void testIsExpired() {
        // 만료된 토큰 생성
        String expiredToken = Jwts.builder()
            .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 1초 전 만료
            .signWith(new SecretKeySpec(secretKey.getBytes(), SignatureAlgorithm.HS256.getJcaName()), SignatureAlgorithm.HS256)
            .compact();

        // 유효한 토큰 생성
        String validToken = Jwts.builder()
            .setExpiration(new Date(System.currentTimeMillis() + 10000)) // 10초 후 만료
            .signWith(new SecretKeySpec(secretKey.getBytes(), SignatureAlgorithm.HS256.getJcaName()), SignatureAlgorithm.HS256)
            .compact();

        // 만료된 토큰 확인 (예외 발생 예상)
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.isExpired(expiredToken));

        // 유효한 토큰 확인
        assertFalse(jwtUtil.isExpired(validToken));
    }

    @Test
    void testInvalidToken() {
        // 잘못된 토큰
        String invalidToken = "invalidToken";

        // 예외 발생 여부 확인
        assertThrows(Exception.class, () -> jwtUtil.getMemberLoginId(invalidToken));
        assertThrows(Exception.class, () -> jwtUtil.getRole(invalidToken));
        assertThrows(Exception.class, () -> jwtUtil.isExpired(invalidToken));
    }
}
