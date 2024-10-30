package com.nhnacademy.ssacthree_gateway.utils;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtUtils {

    private final SecretKey secretKey;

    public JwtUtils(@Value("${spring.jwt.secret}") String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
    }

    // 주어진 토큰의 유효성 검사
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (SignatureException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    // 토큰에서 사용자 id 추출
    public String getIdFromToken(String token) {
        Claims claims = parseClaims(token);
        String id = (String) claims.get("id");
        return id;
    }

    // 토큰에서 사용자 role 추출
    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        String role = (String) claims.get("role");
        return role;
    }

    // 토큰에서 Claims 정보를 파싱하여 반환
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("JWT 토큰의 Claims 파싱에 실패했습니다.", e);
            return null;
        }
    }
    // JwtUtils.java (변경된 부분 추가)
    public String generateToken(String username) {
        return Jwts.builder()
                .claim("username", username)
                .signWith(secretKey)
                .compact();
    }

}
