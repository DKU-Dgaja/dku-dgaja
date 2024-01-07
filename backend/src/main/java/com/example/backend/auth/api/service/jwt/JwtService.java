package com.example.backend.auth.api.service.jwt;

import com.example.backend.common.exception.ExceptionMessage;
import com.example.backend.common.exception.jwt.JwtException;
import com.example.backend.domain.define.user.constant.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secretKey}")
    private String secretKey;

    /*
        JWT AccessToken 생성
    */
    public String generateAccessToken(Map<String, String> customClaims, UserDetails userDetails) {
        return generateAccessToken(customClaims, userDetails, new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24));
    }

    public String generateAccessToken(Map<String, String> customClaims, UserDetails userDetails, Date expiredTime) {
        return Jwts.builder()
                .setClaims(customClaims)
                .setSubject(userDetails.getUsername())  // 메서드명만 Username으로 우리 프로젝트에선 식별자인 email에 해당
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expiredTime)
                .signWith(getSignInkey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /*
        JWT 토큰 정보 추출
    */
    // 모든 Claim 추출
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInkey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 특정 Claim 추출
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);

        return claimsResolver.apply(claims);
    }

    // sub 추출
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 만료 일자 추출
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /*
        JWT 토큰 검증
    */
    public boolean isTokenValid(String token, String username) {
        Claims claims = extractAllClaims(token);

        try {
            if (!claims.containsKey("role")) {
                UserRole.valueOf(claims.get("role", String.class));
                return false;
            }

            if (!claims.containsKey("name")) return false;

            if (!claims.containsKey("profileImageUrl")) return false;

        } catch (RuntimeException e) {
            throw new JwtException(ExceptionMessage.JWT_ILLEGAL_ARGUMENT);
        }

        String subject = claims.getSubject();
        return (subject.equals(username)) && !isTokenExpired(token);
    }

    // JWT 토큰이 만료되었는지 확인
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    // JWT 서명에 사용할 키 획득
    private Key getSignInkey() {
        // Base64로 암호화(인코딩)되어 있는 secretKey를 바이트 배열로 복호화(디코딩)
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);

        // JWT 서명을 위해 HMAC 알고리즘 적용
        return Keys.hmacShaKeyFor(keyBytes);
    }
}