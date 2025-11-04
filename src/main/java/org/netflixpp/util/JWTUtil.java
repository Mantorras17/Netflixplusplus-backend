package org.netflixpp.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JWTUtil {
    private static final Key KEY = Keys.hmacShaKeyFor("VerySecretKeyForNetflixPP_DoNotUseInProduction_ChangeMe123456".getBytes());
    private static final long EXP_MILLIS = 1000L * 60 * 60 * 24; // 24h

    public static String generateToken(String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + EXP_MILLIS))
                .signWith(KEY)
                .compact();
    }

    public static Jws<Claims> parseToken(String token) {
        if (token == null) throw new JwtException("token null");
        String cleaned = token.startsWith("Bearer ") ? token.substring(7) : token;
        return Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(cleaned);
    }

    public static String getUsername(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public static String getRole(String token) {
        Object r = parseToken(token).getBody().get("role");
        return r == null ? null : r.toString();
    }
}
